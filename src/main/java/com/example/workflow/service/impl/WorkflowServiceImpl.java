package com.example.workflow.service.impl;

import com.example.workflow.entity.ApprovalStep;
import com.example.workflow.entity.Request;
import com.example.workflow.entity.User;
import com.example.workflow.enums.HistoryAction;
import com.example.workflow.enums.RequestStatus;
import com.example.workflow.exception.ResourceNotFoundException;
import com.example.workflow.exception.UnauthorizedActionException;
import com.example.workflow.exception.WorkflowTransitionException;
import com.example.workflow.repository.RequestRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.ApprovalHistoryService;
import com.example.workflow.service.WorkflowConfigurationService;
import com.example.workflow.service.WorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final WorkflowConfigurationService configurationService;
    private final ApprovalHistoryService historyService;

    public WorkflowServiceImpl(
            RequestRepository requestRepository,
            UserRepository userRepository,
            WorkflowConfigurationService configurationService,
            ApprovalHistoryService historyService
    ) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.configurationService = configurationService;
        this.historyService = historyService;
    }

    @Override
    public Request processCreation(String requestType, String creator, String serializedPayload) {
        Integer activeVersion = configurationService.getActiveWorkflowVersion(requestType);
        List<ApprovalStep> steps = configurationService.getStepsForWorkflow(requestType, activeVersion);

        Request request = new Request(requestType, creator, serializedPayload, activeVersion);
        request.setStatus(RequestStatus.PENDING);
        request.setCurrentStepOrder(steps.get(0).getStepOrder());

        Request savedRequest = requestRepository.save(request);

        historyService.recordEvent(
                savedRequest.getId(),
                HistoryAction.REQUEST_CREATED,
                creator,
                savedRequest.getCurrentStepOrder(),
                "Request created and workflow initialized at step " + savedRequest.getCurrentStepOrder()
        );

        return savedRequest;
    }

    @Override
    public Request processApproval(Long requestId, String approverUsername, String remarks) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new WorkflowTransitionException(
                    "Cannot approve request " + requestId + ". Current status is " + request.getStatus() + ", expected PENDING.");
        }

        if (request.getCreatedBy().equalsIgnoreCase(approverUsername)) {
            throw new UnauthorizedActionException("A requester cannot approve their own request.");
        }

        ApprovalStep currentStep = configurationService.getStep(
                request.getType(),
                request.getWorkflowVersion(),
                request.getCurrentStepOrder()
        );

        User approver = userRepository.findByUsername(approverUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found: " + approverUsername));

        if (approver.getRole() != currentStep.getRole()) {
            throw new UnauthorizedActionException(
                    "User role '" + approver.getRole() + "' is not authorized to approve step "
                            + currentStep.getStepOrder() + " (required role: '" + currentStep.getRole() + "').");
        }

        historyService.recordEvent(
                request.getId(),
                HistoryAction.APPROVED,
                approverUsername,
                currentStep.getStepOrder(),
                remarks != null ? remarks : "Step " + currentStep.getStepOrder() + " approved"
        );

        Optional<ApprovalStep> nextStepOpt = configurationService.getNextStep(
                request.getType(),
                request.getWorkflowVersion(),
                currentStep.getStepOrder()
        );

        if (nextStepOpt.isPresent()) {
            request.setCurrentStepOrder(nextStepOpt.get().getStepOrder());
        } else {
            request.setStatus(RequestStatus.APPROVED);
            request.setCurrentStepOrder(null);
        }

        return requestRepository.save(request);
    }

    @Override
    public Request processRejection(Long requestId, String rejectorUsername, String remarks) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new WorkflowTransitionException(
                    "Cannot reject request " + requestId + ". Current status is " + request.getStatus() + ", expected PENDING.");
        }

        if (request.getCreatedBy().equalsIgnoreCase(rejectorUsername)) {
            throw new UnauthorizedActionException("A requester cannot reject their own request.");
        }

        ApprovalStep currentStep = configurationService.getStep(
                request.getType(),
                request.getWorkflowVersion(),
                request.getCurrentStepOrder()
        );

        User rejector = userRepository.findByUsername(rejectorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Rejector not found: " + rejectorUsername));

        if (rejector.getRole() != currentStep.getRole()) {
            throw new UnauthorizedActionException(
                    "User role '" + rejector.getRole() + "' is not authorized to reject step "
                            + currentStep.getStepOrder() + " (required role: '" + currentStep.getRole() + "').");
        }

        historyService.recordEvent(
                request.getId(),
                HistoryAction.REJECTED,
                rejectorUsername,
                currentStep.getStepOrder(),
                remarks != null ? remarks : "Step " + currentStep.getStepOrder() + " rejected"
        );

        request.setStatus(RequestStatus.REJECTED);
        request.setCurrentStepOrder(null);

        return requestRepository.save(request);
    }

    @Override
    public Request processAdminOverride(Long requestId, RequestStatus targetStatus, String adminUsername, String remarks) {
        if (targetStatus == RequestStatus.PENDING) {
            throw new WorkflowTransitionException("Administrative override cannot transition a request to PENDING.");
        }

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        if (request.getStatus() == targetStatus) {
            throw new WorkflowTransitionException(
                    "Request " + requestId + " is already in the target status: " + targetStatus + ".");
        }

        HistoryAction action = (targetStatus == RequestStatus.APPROVED)
                ? HistoryAction.ADMIN_OVERRIDE_APPROVED
                : HistoryAction.ADMIN_OVERRIDE_REJECTED;

        historyService.recordEvent(
                request.getId(),
                action,
                adminUsername,
                request.getCurrentStepOrder(),
                remarks
        );

        request.setStatus(targetStatus);
        request.setCurrentStepOrder(null);

        return requestRepository.save(request);
    }
}
