package com.example.workflow.entity;

import com.example.workflow.enums.RequestStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "requests", indexes = {
    @Index(name = "idx_request_created_by", columnList = "created_by"),
    @Index(name = "idx_request_status", columnList = "status"),
    @Index(name = "idx_request_type", columnList = "type")
})
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "current_step_order")
    private Integer currentStepOrder;

    @Column(name = "workflow_version", nullable = false)
    private Integer workflowVersion = 1;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public Request() {
    }

    public Request(String type, String createdBy, String payload, Integer workflowVersion) {
        this.type = type;
        this.createdBy = createdBy;
        this.payload = payload;
        this.workflowVersion = workflowVersion != null ? workflowVersion : 1;
        this.status = RequestStatus.PENDING;
        this.currentStepOrder = 1;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
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

    public Integer getWorkflowVersion() {
        return workflowVersion;
    }

    public void setWorkflowVersion(Integer workflowVersion) {
        this.workflowVersion = workflowVersion;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
