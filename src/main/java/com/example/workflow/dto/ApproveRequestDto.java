package com.example.workflow.dto;

public class ApproveRequestDto {

    private String remarks;

    public ApproveRequestDto() {
    }

    public ApproveRequestDto(String remarks) {
        this.remarks = remarks;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
