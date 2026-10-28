package com.example.workflow.service;

import com.example.workflow.dto.ApprovalHistoryDto;
import com.example.workflow.entity.ApprovalHistory;
import com.example.workflow.enums.HistoryAction;

import java.util.List;

public interface ApprovalHistoryService {

    ApprovalHistory recordEvent(Long requestId, HistoryAction action, String actionBy, Integer stepOrder, String remarks);

    List<ApprovalHistoryDto> getHistoryForRequest(Long requestId, String username, boolean isAdmin);
}
