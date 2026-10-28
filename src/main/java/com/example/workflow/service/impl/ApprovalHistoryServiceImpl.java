package com.example.workflow.service.impl;

import com.example.workflow.dto.ApprovalHistoryDto;
import com.example.workflow.entity.ApprovalHistory;
import com.example.workflow.entity.Request;
import com.example.workflow.enums.HistoryAction;
import com.example.workflow.exception.ResourceNotFoundException;
import com.example.workflow.exception.UnauthorizedActionException;
import com.example.workflow.mapper.DtoMapper;
import com.example.workflow.repository.ApprovalHistoryRepository;
import com.example.workflow.repository.RequestRepository;
import com.example.workflow.service.ApprovalHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApprovalHistoryServiceImpl implements ApprovalHistoryService {

    private final ApprovalHistoryRepository historyRepository;
    private final RequestRepository requestRepository;
    private final DtoMapper dtoMapper;

    public ApprovalHistoryServiceImpl(
            ApprovalHistoryRepository historyRepository,
            RequestRepository requestRepository,
            DtoMapper dtoMapper
    ) {
        this.historyRepository = historyRepository;
        this.requestRepository = requestRepository;
        this.dtoMapper = dtoMapper;
    }

    @Override
    @Transactional
    public ApprovalHistory recordEvent(Long requestId, HistoryAction action, String actionBy, Integer stepOrder, String remarks) {
        ApprovalHistory history = new ApprovalHistory(requestId, action, actionBy, stepOrder, remarks);
        return historyRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalHistoryDto> getHistoryForRequest(Long requestId, String username, boolean isAdmin) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        if (!isAdmin && !request.getCreatedBy().equals(username)) {
            throw new UnauthorizedActionException("Access denied: You may only view history for your own requests.");
        }

        List<ApprovalHistory> histories = historyRepository.findByRequestIdOrderByActionAtAsc(requestId);
        return histories.stream()
                .map(dtoMapper::toApprovalHistoryDto)
                .toList();
    }
}
