package com.example.workflow.dto;

import com.example.workflow.enums.RequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AdminOverrideDto {

    @NotNull(message = "Target status is required (APPROVED or REJECTED)")
    private RequestStatus targetStatus;

    @NotBlank(message = "Mandatory remarks explaining the administrative override reason")
    private String remarks;

    public AdminOverrideDto() {
    }

    public AdminOverrideDto(RequestStatus targetStatus, String remarks) {
        this.targetStatus = targetStatus;
        this.remarks = remarks;
    }

    public RequestStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(RequestStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
