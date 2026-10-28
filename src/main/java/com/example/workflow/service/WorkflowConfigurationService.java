package com.example.workflow.service;

import com.example.workflow.entity.ApprovalStep;
import com.example.workflow.enums.Role;

import java.util.List;
import java.util.Optional;

public interface WorkflowConfigurationService {

    Integer getActiveWorkflowVersion(String requestType);

    List<ApprovalStep> getStepsForWorkflow(String requestType, Integer version);

    ApprovalStep getStep(String requestType, Integer version, Integer stepOrder);

    Optional<ApprovalStep> getNextStep(String requestType, Integer version, Integer currentStepOrder);

    Role getStepRole(String requestType, Integer version, Integer stepOrder);

    boolean isWorkflowConfigured(String requestType);
}
