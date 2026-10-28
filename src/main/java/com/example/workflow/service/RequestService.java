package com.example.workflow.service;

import com.example.workflow.dto.*;

import java.util.List;

public interface RequestService {

    RequestResponseDto createRequest(CreateRequestDto dto);

    RequestResponseDto getRequestById(Long id);

    RequestResponseDto approveRequest(Long id, ApproveRequestDto dto);

    RequestResponseDto rejectRequest(Long id, RejectRequestDto dto);

    RequestResponseDto adminOverride(Long id, AdminOverrideDto dto);

    List<ApprovalHistoryDto> getRequestHistory(Long id);
}
