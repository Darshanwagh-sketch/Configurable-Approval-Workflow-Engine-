package com.example.workflow.service.impl;

import com.example.workflow.entity.ApprovalStep;
import com.example.workflow.enums.Role;
import com.example.workflow.exception.InvalidWorkflowException;
import com.example.workflow.repository.ApprovalStepRepository;
import com.example.workflow.service.WorkflowConfigurationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class WorkflowConfigurationServiceImpl implements WorkflowConfigurationService {

    private final ApprovalStepRepository approvalStepRepository;

    public WorkflowConfigurationServiceImpl(ApprovalStepRepository approvalStepRepository) {
        this.approvalStepRepository = approvalStepRepository;
    }

    @Override
    public Integer getActiveWorkflowVersion(String requestType) {
        if (requestType == null || requestType.isBlank()) {
            throw new InvalidWorkflowException("Request type must not be null or blank");
        }
        return approvalStepRepository.findFirstByRequestTypeAndActiveTrueOrderByWorkflowVersionDesc(requestType)
                .map(ApprovalStep::getWorkflowVersion)
                .orElseThrow(() -> new InvalidWorkflowException(
                        "No active workflow configuration found in the database for request type: " + requestType));
    }

    @Override
    public List<ApprovalStep> getStepsForWorkflow(String requestType, Integer version) {
        if (requestType == null || version == null) {
            throw new InvalidWorkflowException("Request type and workflow version must not be null");
        }
        List<ApprovalStep> steps = approvalStepRepository
                .findByRequestTypeAndWorkflowVersionAndActiveTrueOrderByStepOrderAsc(requestType, version);

        if (steps.isEmpty()) {
            throw new InvalidWorkflowException("No active workflow steps configured for request type: "
                    + requestType + " (version: " + version + ")");
        }

        // Validate sequentially ordered steps starting from 1
        for (int i = 0; i < steps.size(); i++) {
            int expectedOrder = i + 1;
            if (steps.get(i).getStepOrder() != expectedOrder) {
                throw new InvalidWorkflowException("Invalid workflow step sequence for " + requestType
                        + ". Expected step order " + expectedOrder + " but found " + steps.get(i).getStepOrder());
            }
        }

        return steps;
    }

    @Override
    public ApprovalStep getStep(String requestType, Integer version, Integer stepOrder) {
        if (requestType == null || version == null || stepOrder == null) {
            throw new InvalidWorkflowException("Request type, version, and step order must not be null");
        }
        return approvalStepRepository
                .findByRequestTypeAndWorkflowVersionAndStepOrderAndActiveTrue(requestType, version, stepOrder)
                .orElseThrow(() -> new InvalidWorkflowException(
                        "Workflow step " + stepOrder + " not found for request type: " + requestType
                                + " (version: " + version + ")"));
    }

    @Override
    public Optional<ApprovalStep> getNextStep(String requestType, Integer version, Integer currentStepOrder) {
        if (requestType == null || version == null || currentStepOrder == null) {
            return Optional.empty();
        }
        return approvalStepRepository
                .findByRequestTypeAndWorkflowVersionAndStepOrderAndActiveTrue(requestType, version, currentStepOrder + 1);
    }

    @Override
    public Role getStepRole(String requestType, Integer version, Integer stepOrder) {
        if (requestType == null || version == null || stepOrder == null) {
            return null;
        }
        return approvalStepRepository
                .findByRequestTypeAndWorkflowVersionAndStepOrderAndActiveTrue(requestType, version, stepOrder)
                .map(ApprovalStep::getRole)
                .orElse(null);
    }

    @Override
    public boolean isWorkflowConfigured(String requestType) {
        if (requestType == null || requestType.isBlank()) {
            return false;
        }
        return approvalStepRepository.existsByRequestTypeAndActiveTrue(requestType);
    }
}
