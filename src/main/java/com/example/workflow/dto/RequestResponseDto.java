package com.example.workflow.dto;

import com.example.workflow.enums.RequestStatus;
import com.example.workflow.enums.Role;
import java.time.Instant;
import java.util.Map;

public class RequestResponseDto {

    private Long id;
    private String type;
    private RequestStatus status;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer currentStepOrder;
    private Role currentStepRole;
    private Integer workflowVersion;
    private Map<String, Object> payload;

    public RequestResponseDto() {
    }

    public RequestResponseDto(Long id, String type, RequestStatus status, String createdBy,
                              Instant createdAt, Instant updatedAt, Integer currentStepOrder,
                              Role currentStepRole, Integer workflowVersion, Map<String, Object> payload) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.currentStepOrder = currentStepOrder;
        this.currentStepRole = currentStepRole;
        this.workflowVersion = workflowVersion;
        this.payload = payload;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getCurrentStepOrder() {
        return currentStepOrder;
    }

    public void setCurrentStepOrder(Integer currentStepOrder) {
        this.currentStepOrder = currentStepOrder;
    }

    public Role getCurrentStepRole() {
        return currentStepRole;
    }

    public void setCurrentStepRole(Role currentStepRole) {
        this.currentStepRole = currentStepRole;
    }

    public Integer getWorkflowVersion() {
        return workflowVersion;
    }

    public void setWorkflowVersion(Integer workflowVersion) {
        this.workflowVersion = workflowVersion;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
