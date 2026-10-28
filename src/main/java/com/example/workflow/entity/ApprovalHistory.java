package com.example.workflow.entity;

import com.example.workflow.enums.HistoryAction;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "approval_history", indexes = {
    @Index(name = "idx_history_request_id", columnList = "request_id"),
    @Index(name = "idx_history_action_at", columnList = "action_at")
})
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 35)
    private HistoryAction action;

    @Column(name = "action_by", nullable = false, length = 50)
    private String actionBy;

    @Column(name = "action_at", nullable = false, updatable = false)
    private Instant actionAt = Instant.now();

    @Column(name = "step_order")
    private Integer stepOrder;

    @Column(length = 1000)
    private String remarks;

    public ApprovalHistory() {
    }

    public ApprovalHistory(Long requestId, HistoryAction action, String actionBy, Integer stepOrder, String remarks) {
        this.requestId = requestId;
        this.action = action;
        this.actionBy = actionBy;
        this.stepOrder = stepOrder;
        this.remarks = remarks;
        this.actionAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public HistoryAction getAction() {
        return action;
    }

    public void setAction(HistoryAction action) {
        this.action = action;
    }

    public String getActionBy() {
        return actionBy;
    }

    public void setActionBy(String actionBy) {
        this.actionBy = actionBy;
    }

    public Instant getActionAt() {
        return actionAt;
    }

    public void setActionAt(Instant actionAt) {
        this.actionAt = actionAt;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
