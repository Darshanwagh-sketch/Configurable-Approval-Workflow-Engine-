package com.example.workflow.entity;

import com.example.workflow.dto.*;
import com.example.workflow.enums.HistoryAction;
import com.example.workflow.enums.RequestStatus;
import com.example.workflow.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DomainModelTest {

    @Test
    @DisplayName("Domain Entities: User getters, setters, and properties")
    void testUserEntity() {
        User user = new User();
        user.setId(1L);
        user.setUsername("bob");
        user.setPasswordHash("hash123");
        user.setRole(Role.APPROVER);
        user.setEnabled(true);
        Instant now = Instant.now();
        user.setCreatedAt(now);

        assertEquals(1L, user.getId());
        assertEquals("bob", user.getUsername());
        assertEquals("hash123", user.getPasswordHash());
        assertEquals(Role.APPROVER, user.getRole());
        assertTrue(user.isEnabled());
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    @DisplayName("Domain Entities: ApprovalStep getters, setters, and constructors")
    void testApprovalStepEntity() {
        ApprovalStep step = new ApprovalStep();
        step.setId(5L);
        step.setRequestType("CUSTOM_TYPE");
        step.setStepOrder(1);
        step.setRole(Role.ADMIN);
        step.setWorkflowVersion(2);
        step.setActive(true);

        assertEquals(5L, step.getId());
        assertEquals("CUSTOM_TYPE", step.getRequestType());
        assertEquals(1, step.getStepOrder());
        assertEquals(Role.ADMIN, step.getRole());
        assertEquals(2, step.getWorkflowVersion());
        assertTrue(step.isActive());

        ApprovalStep step2 = new ApprovalStep("LEAVE", 1, Role.APPROVER);
        assertEquals(1, step2.getWorkflowVersion());
        assertTrue(step2.isActive());
    }

    @Test
    @DisplayName("Domain Entities: Request getters, setters, lifecycle updates, and versioning")
    void testRequestEntity() {
        Request req = new Request();
        req.setId(10L);
        req.setType("EXPENSE");
        req.setStatus(RequestStatus.PENDING);
        req.setCreatedBy("alice");
        req.setCurrentStepOrder(2);
        req.setWorkflowVersion(1);
        req.setPayload("{\"amount\":100}");
        req.setVersion(3L);
        Instant now = Instant.now();
        req.setCreatedAt(now);
        req.setUpdatedAt(now);
        req.onUpdate();

        assertEquals(10L, req.getId());
        assertEquals("EXPENSE", req.getType());
        assertEquals(RequestStatus.PENDING, req.getStatus());
        assertEquals("alice", req.getCreatedBy());
        assertEquals(2, req.getCurrentStepOrder());
        assertEquals(1, req.getWorkflowVersion());
        assertEquals("{\"amount\":100}", req.getPayload());
        assertEquals(3L, req.getVersion());
        assertEquals(now, req.getCreatedAt());
        assertNotNull(req.getUpdatedAt());
    }

    @Test
    @DisplayName("Domain Entities: ApprovalHistory getters, setters, and audit immutability")
    void testApprovalHistoryEntity() {
        ApprovalHistory history = new ApprovalHistory();
        history.setId(50L);
        history.setRequestId(10L);
        history.setAction(HistoryAction.REQUEST_CREATED);
        history.setActionBy("alice");
        Instant now = Instant.now();
        history.setActionAt(now);
        history.setStepOrder(1);
        history.setRemarks("Created");

        assertEquals(50L, history.getId());
        assertEquals(10L, history.getRequestId());
        assertEquals(HistoryAction.REQUEST_CREATED, history.getAction());
        assertEquals("alice", history.getActionBy());
        assertEquals(now, history.getActionAt());
        assertEquals(1, history.getStepOrder());
        assertEquals("Created", history.getRemarks());
    }

    @Test
    @DisplayName("DTOs: Accessor coverage across all request and response models")
    void testDtoAccessors() {
        AuthResponse authRes = new AuthResponse();
        authRes.setToken("tok123");
        authRes.setTokenType("Bearer");
        authRes.setUsername("testuser");
        authRes.setRole(Role.REQUESTER);
        authRes.setExpiresInMs(3600);

        assertEquals("tok123", authRes.getToken());
        assertEquals("Bearer", authRes.getTokenType());
        assertEquals("testuser", authRes.getUsername());
        assertEquals(Role.REQUESTER, authRes.getRole());
        assertEquals(3600, authRes.getExpiresInMs());

        ErrorResponse err = new ErrorResponse();
        Instant now = Instant.now();
        err.setTimestamp(now);
        err.setStatus(404);
        err.setError("Not Found");
        err.setMessage("Item missing");
        err.setPath("/item");

        assertEquals(now, err.getTimestamp());
        assertEquals(404, err.getStatus());
        assertEquals("Not Found", err.getError());
        assertEquals("Item missing", err.getMessage());
        assertEquals("/item", err.getPath());

        ApproveRequestDto approveDto = new ApproveRequestDto();
        approveDto.setRemarks("LGTM");
        assertEquals("LGTM", approveDto.getRemarks());

        RejectRequestDto rejectDto = new RejectRequestDto();
        rejectDto.setRemarks("Deny");
        assertEquals("Deny", rejectDto.getRemarks());

        AdminOverrideDto overrideDto = new AdminOverrideDto();
        overrideDto.setTargetStatus(RequestStatus.APPROVED);
        overrideDto.setRemarks("Override");
        assertEquals(RequestStatus.APPROVED, overrideDto.getTargetStatus());
        assertEquals("Override", overrideDto.getRemarks());

        RequestResponseDto resDto = new RequestResponseDto();
        resDto.setId(1L);
        resDto.setType("LEAVE");
        resDto.setStatus(RequestStatus.APPROVED);
        resDto.setCreatedBy("john");
        resDto.setCreatedAt(now);
        resDto.setUpdatedAt(now);
        resDto.setCurrentStepOrder(null);
        resDto.setCurrentStepRole(null);
        resDto.setWorkflowVersion(1);
        resDto.setPayload(Map.of("k", "v"));

        assertEquals(1L, resDto.getId());
        assertEquals("LEAVE", resDto.getType());
        assertEquals(RequestStatus.APPROVED, resDto.getStatus());
        assertEquals("john", resDto.getCreatedBy());
        assertEquals(now, resDto.getCreatedAt());
        assertEquals(now, resDto.getUpdatedAt());
        assertNull(resDto.getCurrentStepOrder());
        assertNull(resDto.getCurrentStepRole());
        assertEquals(1, resDto.getWorkflowVersion());
        assertEquals("v", resDto.getPayload().get("k"));
    }
}
