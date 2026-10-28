package com.example.workflow.dto;

public class RejectRequestDto {

    private String remarks;

    public RejectRequestDto() {
    }

    public RejectRequestDto(String remarks) {
        this.remarks = remarks;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
