package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class CreateRequestDto {

    @NotBlank(message = "Request type is required")
    private String type;

    private Map<String, Object> payload;

    public CreateRequestDto() {
    }

    public CreateRequestDto(String type, Map<String, Object> payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
