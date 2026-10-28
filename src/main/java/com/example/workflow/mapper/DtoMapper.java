package com.example.workflow.mapper;

import com.example.workflow.dto.ApprovalHistoryDto;
import com.example.workflow.dto.RequestResponseDto;
import com.example.workflow.entity.ApprovalHistory;
import com.example.workflow.entity.Request;
import com.example.workflow.enums.Role;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class DtoMapper {

    private final ObjectMapper objectMapper;

    public DtoMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RequestResponseDto toRequestResponseDto(Request request, Role currentStepRole) {
        if (request == null) {
            return null;
        }

        Map<String, Object> payloadMap = null;
        if (request.getPayload() != null && !request.getPayload().isBlank()) {
            try {
                payloadMap = objectMapper.readValue(request.getPayload(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                payloadMap = Collections.singletonMap("raw", request.getPayload());
            }
        }

        return new RequestResponseDto(
                request.getId(),
                request.getType(),
                request.getStatus(),
                request.getCreatedBy(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getCurrentStepOrder(),
                currentStepRole,
                request.getWorkflowVersion(),
                payloadMap
        );
    }

    public ApprovalHistoryDto toApprovalHistoryDto(ApprovalHistory history) {
        if (history == null) {
            return null;
        }
        return new ApprovalHistoryDto(
                history.getId(),
                history.getRequestId(),
                history.getAction(),
                history.getActionBy(),
                history.getActionAt(),
                history.getStepOrder(),
                history.getRemarks()
        );
    }

    public String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid payload format", e);
        }
    }
}
