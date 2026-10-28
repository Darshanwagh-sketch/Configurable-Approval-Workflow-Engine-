package com.example.workflow.service;

import com.example.workflow.entity.ApprovalStep;
import com.example.workflow.enums.Role;
import com.example.workflow.exception.InvalidWorkflowException;
import com.example.workflow.repository.ApprovalStepRepository;
import com.example.workflow.service.impl.WorkflowConfigurationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkflowConfigurationServiceTest {

    @Mock
    private ApprovalStepRepository repository;

    private WorkflowConfigurationService service;

    @BeforeEach
    void setUp() {
        service = new WorkflowConfigurationServiceImpl(repository);
    }

    @Test
    @DisplayName("Configuration: Throws exception when requestType is null or blank")
    void testBlankTypeValidation() {
        assertThrows(InvalidWorkflowException.class, () -> service.getActiveWorkflowVersion(null));
        assertThrows(InvalidWorkflowException.class, () -> service.getActiveWorkflowVersion("   "));
        assertThrows(InvalidWorkflowException.class, () -> service.getStepsForWorkflow(null, 1));
        assertThrows(InvalidWorkflowException.class, () -> service.getStep(null, 1, 1));
        assertFalse(service.isWorkflowConfigured(null));
        assertFalse(service.isWorkflowConfigured(""));
    }

    @Test
    @DisplayName("Configuration: Throws exception when sequence has gaps or does not start at 1")
    void testInvalidStepSequenceValidation() {
        // Step order starting at 2 instead of 1
        ApprovalStep step2 = new ApprovalStep("CUSTOM", 2, Role.ADMIN, 1, true);
        when(repository.findByRequestTypeAndWorkflowVersionAndActiveTrueOrderByStepOrderAsc("CUSTOM", 1))
                .thenReturn(List.of(step2));

        InvalidWorkflowException ex = assertThrows(InvalidWorkflowException.class, () ->
                service.getStepsForWorkflow("CUSTOM", 1)
        );
        assertTrue(ex.getMessage().contains("Expected step order 1 but found 2"));
    }

    @Test
    @DisplayName("Configuration: Correctly retrieves active version and next steps")
    void testNextStepResolution() {
        ApprovalStep step1 = new ApprovalStep("EXPENSE", 1, Role.APPROVER, 1, true);
        ApprovalStep step2 = new ApprovalStep("EXPENSE", 2, Role.ADMIN, 1, true);

        when(repository.findFirstByRequestTypeAndActiveTrueOrderByWorkflowVersionDesc("EXPENSE"))
                .thenReturn(Optional.of(step1));
        when(repository.findByRequestTypeAndWorkflowVersionAndStepOrderAndActiveTrue("EXPENSE", 1, 2))
                .thenReturn(Optional.of(step2));

        assertEquals(1, service.getActiveWorkflowVersion("EXPENSE"));
        Optional<ApprovalStep> nextStep = service.getNextStep("EXPENSE", 1, 1);
        assertTrue(nextStep.isPresent());
        assertEquals(2, nextStep.get().getStepOrder());
        assertEquals(Role.ADMIN, nextStep.get().getRole());

        // When at last step (step 2), next step is empty
        when(repository.findByRequestTypeAndWorkflowVersionAndStepOrderAndActiveTrue("EXPENSE", 1, 3))
                .thenReturn(Optional.empty());
        Optional<ApprovalStep> terminalNext = service.getNextStep("EXPENSE", 1, 2);
        assertTrue(terminalNext.isEmpty());
    }

    @Test
    @DisplayName("Configuration: Helper methods handle null parameters gracefully")
    void testNullParameterHandling() {
        assertNull(service.getStepRole(null, 1, 1));
        assertNull(service.getStepRole("LEAVE", null, 1));
        assertNull(service.getStepRole("LEAVE", 1, null));
        assertTrue(service.getNextStep(null, 1, 1).isEmpty());
        assertTrue(service.getNextStep("LEAVE", null, 1).isEmpty());
        assertTrue(service.getNextStep("LEAVE", 1, null).isEmpty());
    }
}
