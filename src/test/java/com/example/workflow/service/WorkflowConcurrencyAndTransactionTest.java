package com.example.workflow.service;

import com.example.workflow.entity.ApprovalHistory;
import com.example.workflow.entity.Request;
import com.example.workflow.enums.HistoryAction;
import com.example.workflow.enums.RequestStatus;
import com.example.workflow.exception.WorkflowTransitionException;
import com.example.workflow.repository.ApprovalHistoryRepository;
import com.example.workflow.repository.RequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
public class WorkflowConcurrencyAndTransactionTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private RequestRepository requestRepository;

    @SpyBean
    private ApprovalHistoryRepository historyRepository;

    @Test
    @DisplayName("Concurrency: Concurrent approval attempts cannot advance the same step twice")
    void testConcurrentApprovalsPreventDoubleAdvancement() throws Exception {
        // Create request
        Request request = workflowService.processCreation("LEAVE", "requester1", "{\"days\": 2}");
        Long requestId = request.getId();

        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    workflowService.processApproval(requestId, "approver1", "Concurrent approval attempt " + threadIndex);
                    successCount.incrementAndGet();
                } catch (WorkflowTransitionException | OptimisticLockingFailureException e) {
                    conflictCount.incrementAndGet();
                    exceptions.add(e);
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire both threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        assertTrue(completed, "Concurrent threads should complete execution within timeout");
        assertEquals(1, successCount.get(), "Exactly one approval transaction must succeed");
        assertEquals(1, conflictCount.get(), "Exactly one approval transaction must fail with conflict/optimistic locking exception");

        // Verify request advanced only once (step 1 -> step 2)
        Request finalRequest = requestRepository.findById(requestId).orElseThrow();
        assertEquals(Integer.valueOf(2), finalRequest.getCurrentStepOrder(), "Request must only have advanced to step 2 once");
        assertEquals(RequestStatus.PENDING, finalRequest.getStatus());
    }

    @Test
    @DisplayName("Transaction Rollback: Failure during audit persistence rolls back state transition atomically")
    void testTransactionRollbackOnAuditFailure() {
        // 1. Create a request
        Request request = workflowService.processCreation("EXPENSE", "requester1", "{\"amount\": 150}");
        Long requestId = request.getId();
        Integer initialStep = request.getCurrentStepOrder();
        RequestStatus initialStatus = request.getStatus();

        // 2. Injected failure on historyRepository during audit save
        doThrow(new RuntimeException("Simulated audit disk failure"))
                .when(historyRepository)
                .save(any(ApprovalHistory.class));

        // 3. Attempt approval, expect RuntimeException
        assertThrows(RuntimeException.class, () ->
                workflowService.processApproval(requestId, "approver1", "Should fail and rollback")
        );

        // 4. Verify request was rolled back completely and remains at initial state
        Request postFailureRequest = requestRepository.findById(requestId).orElseThrow();
        assertEquals(initialStatus, postFailureRequest.getStatus(), "Status must not be updated if audit fails");
        assertEquals(initialStep, postFailureRequest.getCurrentStepOrder(), "Step must not be advanced if audit fails");

        // 5. Verify no APPROVED history was persisted
        List<ApprovalHistory> histories = historyRepository.findByRequestIdOrderByActionAtAsc(requestId);
        assertEquals(1, histories.size(), "Only the initial creation audit should exist");
        assertEquals(HistoryAction.REQUEST_CREATED, histories.get(0).getAction());
    }

    @Test
    @DisplayName("Integrity: Invalid transitions do not alter request state or append history")
    void testInvalidTransitionIntegrity() {
        Request request = workflowService.processCreation("QUICK_REQUEST", "requester1", null);
        Long requestId = request.getId();

        // Approve it so it becomes APPROVED
        workflowService.processApproval(requestId, "approver1", "First approve");
        Request approved = requestRepository.findById(requestId).orElseThrow();
        assertEquals(RequestStatus.APPROVED, approved.getStatus());

        int historyCountBefore = historyRepository.findByRequestIdOrderByActionAtAsc(requestId).size();

        // Attempt second approval
        assertThrows(WorkflowTransitionException.class, () ->
                workflowService.processApproval(requestId, "approver1", "Second approve should fail")
        );

        // Verify request and history remain unaffected
        Request unchanged = requestRepository.findById(requestId).orElseThrow();
        assertEquals(RequestStatus.APPROVED, unchanged.getStatus());
        int historyCountAfter = historyRepository.findByRequestIdOrderByActionAtAsc(requestId).size();
        assertEquals(historyCountBefore, historyCountAfter, "No new history record should be appended on invalid transition");
    }
}
