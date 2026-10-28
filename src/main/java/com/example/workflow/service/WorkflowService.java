package com.example.workflow.service;

import com.example.workflow.entity.Request;
import com.example.workflow.enums.RequestStatus;

public interface WorkflowService {

    Request processCreation(String requestType, String creator, String serializedPayload);

    Request processApproval(Long requestId, String approverUsername, String remarks);

    Request processRejection(Long requestId, String rejectorUsername, String remarks);

    Request processAdminOverride(Long requestId, RequestStatus targetStatus, String adminUsername, String remarks);
}
