package com.example.workflow.controller;

import com.example.workflow.dto.*;
import com.example.workflow.enums.RequestStatus;
import com.example.workflow.enums.Role;
import com.example.workflow.repository.RequestRepository;
import com.example.workflow.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RequestWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RequestRepository requestRepository;

    private String requester1Token;
    private String requester2Token;
    private String approver1Token;
    private String approver2Token;
    private String adminToken;

    @BeforeEach
    void setUp() {
        requester1Token = "Bearer " + jwtService.generateToken("requester1", Role.REQUESTER);
        requester2Token = "Bearer " + jwtService.generateToken("requester2", Role.REQUESTER);
        approver1Token = "Bearer " + jwtService.generateToken("approver1", Role.APPROVER);
        approver2Token = "Bearer " + jwtService.generateToken("approver2", Role.APPROVER);
        adminToken = "Bearer " + jwtService.generateToken("admin", Role.ADMIN);
    }

    @Test
    @DisplayName("Security: Unauthenticated access returns 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/requests/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("Security: APPROVER cannot create request (403 Forbidden)")
    void testApproverCannotCreateRequest() throws Exception {
        CreateRequestDto dto = new CreateRequestDto("LEAVE", Map.of("reason", "Vacation"));

        mockMvc.perform(post("/requests")
                        .header("Authorization", approver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Creation: REQUESTER creates LEAVE request successfully (201 Created)")
    void testCreateLeaveRequestSuccess() throws Exception {
        CreateRequestDto dto = new CreateRequestDto("LEAVE", Map.of("days", 3, "reason", "Sick leave"));

        mockMvc.perform(post("/requests")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.type", is("LEAVE")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.createdBy", is("requester1")))
                .andExpect(jsonPath("$.currentStepOrder", is(1)))
                .andExpect(jsonPath("$.currentStepRole", is("APPROVER")))
                .andExpect(jsonPath("$.payload.days", is(3)));
    }

    @Test
    @DisplayName("Creation: Fails when workflow configuration is missing (400 Bad Request)")
    void testCreateRequestWithUnknownType() throws Exception {
        CreateRequestDto dto = new CreateRequestDto("NON_EXISTENT_WORKFLOW", Map.of("data", 123));

        mockMvc.perform(post("/requests")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid Workflow Configuration")));
    }

    @Test
    @DisplayName("Creation: Fails on blank request type (400 Bad Request)")
    void testCreateRequestBlankType() throws Exception {
        CreateRequestDto dto = new CreateRequestDto("", Map.of());

        mockMvc.perform(post("/requests")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")));
    }

    @Test
    @DisplayName("Retrieval: Requester can access own request; Other requester is forbidden (403)")
    void testRequestOwnershipAccessRestrictions() throws Exception {
        // Create request as requester1
        CreateRequestDto dto = new CreateRequestDto("LEAVE", Map.of("note", "private leave"));
        MvcResult result = mockMvc.perform(post("/requests")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        RequestResponseDto created = objectMapper.readValue(result.getResponse().getContentAsString(), RequestResponseDto.class);
        Long requestId = created.getId();

        // Creator (requester1) accesses -> 200 OK
        mockMvc.perform(get("/requests/" + requestId)
                        .header("Authorization", requester1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdBy", is("requester1")));

        // Different requester (requester2) accesses -> 403 Forbidden
        mockMvc.perform(get("/requests/" + requestId)
                        .header("Authorization", requester2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Forbidden Action")));

        // APPROVER accesses -> 200 OK
        mockMvc.perform(get("/requests/" + requestId)
                        .header("Authorization", approver1Token))
                .andExpect(status().isOk());

        // ADMIN accesses -> 200 OK
        mockMvc.perform(get("/requests/" + requestId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Retrieval: Non-existent request returns 404 Not Found")
    void testGetNonExistentRequest() throws Exception {
        mockMvc.perform(get("/requests/999999")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Resource Not Found")));
    }

    @Test
    @DisplayName("Approval: Full 2-step workflow execution (LEAVE: Step 1 APPROVER -> Step 2 ADMIN -> APPROVED)")
    void testFullTwoStepWorkflowExecution() throws Exception {
        // 1. Create LEAVE request
        CreateRequestDto dto = new CreateRequestDto("LEAVE", Map.of("reason", "Annual Holiday"));
        MvcResult createRes = mockMvc.perform(post("/requests")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        RequestResponseDto req = objectMapper.readValue(createRes.getResponse().getContentAsString(), RequestResponseDto.class);
        Long id = req.getId();

        // 2. Requester tries self-approval -> 403 Forbidden
        mockMvc.perform(post("/requests/" + id + "/approve")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRequestDto("Self approve"))))
                .andExpect(status().isForbidden());

        // 3. Approver 1 approves Step 1 -> Advances to Step 2 (Admin)
        mockMvc.perform(post("/requests/" + id + "/approve")
                        .header("Authorization", approver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRequestDto("Manager approval"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.currentStepOrder", is(2)))
                .andExpect(jsonPath("$.currentStepRole", is("ADMIN")));

        // 4. Approver tries to approve Step 2 (which requires ADMIN) -> 403 Forbidden
        mockMvc.perform(post("/requests/" + id + "/approve")
                        .header("Authorization", approver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRequestDto("Try step 2"))))
                .andExpect(status().isForbidden());

        // 5. Admin approves Step 2 -> Workflow completes, status becomes APPROVED
        mockMvc.perform(post("/requests/" + id + "/approve")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRequestDto("Executive sign-off"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.currentStepOrder", nullValue()))
                .andExpect(jsonPath("$.currentStepRole", nullValue()));

        // 6. Attempting to approve already APPROVED request returns 409 Conflict
        mockMvc.perform(post("/requests/" + id + "/approve")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRequestDto("Double approval"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Invalid State Transition")));
    }

    @Test
    @DisplayName("Rejection: Approver rejects pending request -> REJECTED status, workflow terminated")
    void testRejectionWorkflow() throws Exception {
        CreateRequestDto dto = new CreateRequestDto("EXPENSE", Map.of("amount", 500));
        MvcResult createRes = mockMvc.perform(post("/requests")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        RequestResponseDto req = objectMapper.readValue(createRes.getResponse().getContentAsString(), RequestResponseDto.class);
        Long id = req.getId();

        // Requester tries self-rejection -> 403 Forbidden
        mockMvc.perform(post("/requests/" + id + "/reject")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectRequestDto("Self reject"))))
                .andExpect(status().isForbidden());

        // Approver rejects -> 200 OK, status REJECTED
        mockMvc.perform(post("/requests/" + id + "/reject")
                        .header("Authorization", approver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectRequestDto("Exceeds budget allowance"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.currentStepOrder", nullValue()));

        // Attempting to reject already REJECTED request returns 409 Conflict
        mockMvc.perform(post("/requests/" + id + "/reject")
                        .header("Authorization", approver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectRequestDto("Reject again"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Invalid State Transition")));
    }

    @Test
    @DisplayName("Multi-step Workflow: 3-step workflow (PURCHASE_ORDER) processes seamlessly")
    void testThreeStepPurchaseOrderWorkflow() throws Exception {
        CreateRequestDto dto = new CreateRequestDto("PURCHASE_ORDER", Map.of("item", "Laptops", "cost", 10000));
        MvcResult createRes = mockMvc.perform(post("/requests")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentStepOrder", is(1)))
                .andExpect(jsonPath("$.currentStepRole", is("APPROVER")))
                .andReturn();
        RequestResponseDto req = objectMapper.readValue(createRes.getResponse().getContentAsString(), RequestResponseDto.class);
        Long id = req.getId();

        // Step 1: Approver 1 approves -> Advances to Step 2
        mockMvc.perform(post("/requests/" + id + "/approve")
                        .header("Authorization", approver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRequestDto("Dept manager ok"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStepOrder", is(2)))
                .andExpect(jsonPath("$.currentStepRole", is("APPROVER")));

        // Step 2: Approver 2 approves -> Advances to Step 3
        mockMvc.perform(post("/requests/" + id + "/approve")
                        .header("Authorization", approver2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRequestDto("Finance manager ok"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStepOrder", is(3)))
                .andExpect(jsonPath("$.currentStepRole", is("ADMIN")));

        // Step 3: Admin approves -> Workflow complete
        mockMvc.perform(post("/requests/" + id + "/approve")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRequestDto("Director approval"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.currentStepOrder", nullValue()));
    }

    @Test
    @DisplayName("Single-step Workflow: 1-step workflow (QUICK_REQUEST) completes on first approval")
    void testSingleStepWorkflowExecution() throws Exception {
        CreateRequestDto dto = new CreateRequestDto("QUICK_REQUEST", Map.of("action", "Fast track"));
        MvcResult createRes = mockMvc.perform(post("/requests")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentStepOrder", is(1)))
                .andReturn();
        RequestResponseDto req = objectMapper.readValue(createRes.getResponse().getContentAsString(), RequestResponseDto.class);

        mockMvc.perform(post("/requests/" + req.getId() + "/approve")
                        .header("Authorization", approver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRequestDto("Approved fast"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.currentStepOrder", nullValue()));
    }
}
