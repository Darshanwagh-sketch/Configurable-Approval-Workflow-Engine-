package com.example.workflow.service.impl;

import com.example.workflow.dto.*;
import com.example.workflow.entity.Request;
import com.example.workflow.enums.Role;
import com.example.workflow.exception.ResourceNotFoundException;
import com.example.workflow.exception.UnauthorizedActionException;
import com.example.workflow.mapper.DtoMapper;
import com.example.workflow.repository.RequestRepository;
import com.example.workflow.security.SecurityUtils;
import com.example.workflow.service.ApprovalHistoryService;
import com.example.workflow.service.RequestService;
import com.example.workflow.service.WorkflowConfigurationService;
import com.example.workflow.service.WorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final WorkflowService workflowService;
    private final WorkflowConfigurationService configurationService;
    private final ApprovalHistoryService historyService;
    private final DtoMapper dtoMapper;

    public RequestServiceImpl(
            RequestRepository requestRepository,
            WorkflowService workflowService,
            WorkflowConfigurationService configurationService,
            ApprovalHistoryService historyService,
            DtoMapper dtoMapper
    ) {
        this.requestRepository = requestRepository;
        this.workflowService = workflowService;
        this.configurationService = configurationService;
        this.historyService = historyService;
        this.dtoMapper = dtoMapper;
    }

    @Override
    @Transactional
    public RequestResponseDto createRequest(CreateRequestDto dto) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        String serializedPayload = dtoMapper.serializePayload(dto.getPayload());

        Request request = workflowService.processCreation(dto.getType(), currentUsername, serializedPayload);
        Role stepRole = configurationService.getStepRole(
                request.getType(),
                request.getWorkflowVersion(),
                request.getCurrentStepOrder()
        );

        return dtoMapper.toRequestResponseDto(request, stepRole);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestResponseDto getRequestById(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));

        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean isPrivileged = SecurityUtils.hasRole(Role.ADMIN) || SecurityUtils.hasRole(Role.APPROVER);

        if (!isPrivileged && !request.getCreatedBy().equals(currentUsername)) {
            throw new UnauthorizedActionException("Access denied: You may only access your own requests.");
        }

        Role stepRole = configurationService.getStepRole(
                request.getType(),
                request.getWorkflowVersion(),
                request.getCurrentStepOrder()
        );

        return dtoMapper.toRequestResponseDto(request, stepRole);
    }

    @Override
    @Transactional
    public RequestResponseDto approveRequest(Long id, ApproveRequestDto dto) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        String remarks = dto != null ? dto.getRemarks() : null;

        Request updated = workflowService.processApproval(id, currentUsername, remarks);
        Role stepRole = configurationService.getStepRole(
                updated.getType(),
                updated.getWorkflowVersion(),
                updated.getCurrentStepOrder()
        );

        return dtoMapper.toRequestResponseDto(updated, stepRole);
    }

    @Override
    @Transactional
    public RequestResponseDto rejectRequest(Long id, RejectRequestDto dto) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        String remarks = dto != null ? dto.getRemarks() : null;

        Request updated = workflowService.processRejection(id, currentUsername, remarks);
        Role stepRole = configurationService.getStepRole(
                updated.getType(),
                updated.getWorkflowVersion(),
                updated.getCurrentStepOrder()
        );

        return dtoMapper.toRequestResponseDto(updated, stepRole);
    }

    @Override
    @Transactional
    public RequestResponseDto adminOverride(Long id, AdminOverrideDto dto) {
        String currentUsername = SecurityUtils.getCurrentUsername();

        Request updated = workflowService.processAdminOverride(
                id,
                dto.getTargetStatus(),
                currentUsername,
                dto.getRemarks()
        );

        Role stepRole = configurationService.getStepRole(
                updated.getType(),
                updated.getWorkflowVersion(),
                updated.getCurrentStepOrder()
        );

        return dtoMapper.toRequestResponseDto(updated, stepRole);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalHistoryDto> getRequestHistory(Long id) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean isAdmin = SecurityUtils.hasRole(Role.ADMIN);
        return historyService.getHistoryForRequest(id, currentUsername, isAdmin);
    }
}
