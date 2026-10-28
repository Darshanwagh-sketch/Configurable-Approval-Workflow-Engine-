package com.example.workflow.dto;

import com.example.workflow.enums.HistoryAction;
import java.time.Instant;

public class ApprovalHistoryDto {

    private Long id;
    private Long requestId;
    private HistoryAction action;
    private String actionBy;
    private Instant actionAt;
    private Integer stepOrder;
    private String remarks;

    public ApprovalHistoryDto() {
    }

    public ApprovalHistoryDto(Long id, Long requestId, HistoryAction action, String actionBy,
                              Instant actionAt, Integer stepOrder, String remarks) {
        this.id = id;
        this.requestId = requestId;
        this.action = action;
        this.actionBy = actionBy;
        this.actionAt = actionAt;
        this.stepOrder = stepOrder;
        this.remarks = remarks;
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
