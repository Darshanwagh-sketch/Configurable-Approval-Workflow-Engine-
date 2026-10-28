package com.example.workflow.controller;

import com.example.workflow.dto.AdminOverrideDto;
import com.example.workflow.dto.CreateRequestDto;
import com.example.workflow.dto.RequestResponseDto;
import com.example.workflow.enums.RequestStatus;
import com.example.workflow.enums.Role;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminOverrideIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private String requesterToken;
    private String approverToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        requesterToken = "Bearer " + jwtService.generateToken("requester1", Role.REQUESTER);
        approverToken = "Bearer " + jwtService.generateToken("approver1", Role.APPROVER);
        adminToken = "Bearer " + jwtService.generateToken("admin", Role.ADMIN);
    }

    @Test
    @DisplayName("Admin Override: Admin overrides PENDING request to APPROVED with remarks (200 OK)")
    void testAdminOverridePendingToApproved() throws Exception {
        CreateRequestDto dto = new CreateRequestDto("LEAVE", Map.of("reason", "Urgent executive travel"));
        MvcResult createRes = mockMvc.perform(post("/requests")
                        .header("Authorization", requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        RequestResponseDto req = objectMapper.readValue(createRes.getResponse().getContentAsString(), RequestResponseDto.class);
        Long id = req.getId();

        AdminOverrideDto overrideDto = new AdminOverrideDto(RequestStatus.APPROVED, "Executive director waiver applied");

        mockMvc.perform(post("/admin/requests/" + id + "/override")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overrideDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.currentStepOrder", nullValue()));

        // Verify history shows ADMIN_OVERRIDE_APPROVED
        mockMvc.perform(get("/requests/history/" + id)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].action", is("ADMIN_OVERRIDE_APPROVED")))
                .andExpect(jsonPath("$[1].actionBy", is("admin")))
                .andExpect(jsonPath("$[1].remarks", is("Executive director waiver applied")));
    }

    @Test
    @DisplayName("Admin Override: Admin overrides APPROVED request to REJECTED (200 OK)")
    void testAdminOverrideApprovedToRejected() throws Exception {
        CreateRequestDto dto = new CreateRequestDto("QUICK_REQUEST", Map.of("reason", "Expedited request"));
        MvcResult createRes = mockMvc.perform(post("/requests")
                        .header("Authorization", requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        RequestResponseDto req = objectMapper.readValue(createRes.getResponse().getContentAsString(), RequestResponseDto.class);
        Long id = req.getId();

        // Approve it first
        mockMvc.perform(post("/requests/" + id + "/approve")
                        .header("Authorization", approverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // Admin overrides approved request to REJECTED
        AdminOverrideDto overrideDto = new AdminOverrideDto(RequestStatus.REJECTED, "Post-approval audit discovered discrepancy");
        mockMvc.perform(post("/admin/requests/" + id + "/override")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overrideDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));
    }

    @Test
    @DisplayName("Admin Override: Idempotency check - Overriding to same status returns 409 Conflict")
    void testAdminOverrideDuplicateStatusConflict() throws Exception {
        CreateRequestDto dto = new CreateRequestDto("QUICK_REQUEST", Map.of());
        MvcResult createRes = mockMvc.perform(post("/requests")
                        .header("Authorization", requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        RequestResponseDto req = objectMapper.readValue(createRes.getResponse().getContentAsString(), RequestResponseDto.class);
        Long id = req.getId();

        // Override to APPROVED
        mockMvc.perform(post("/admin/requests/" + id + "/override")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminOverrideDto(RequestStatus.APPROVED, "First override"))))
                .andExpect(status().isOk());

        // Override to APPROVED again -> 409 Conflict
        mockMvc.perform(post("/admin/requests/" + id + "/override")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminOverrideDto(RequestStatus.APPROVED, "Repeated override"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Invalid State Transition")));
    }

    @Test
    @DisplayName("Admin Override: Non-admin users are strictly forbidden (403 Forbidden)")
    void testNonAdminCannotInvokeOverride() throws Exception {
        AdminOverrideDto dto = new AdminOverrideDto(RequestStatus.APPROVED, "Unauthorized attempt");

        // Requester tries -> 403
        mockMvc.perform(post("/admin/requests/1/override")
                        .header("Authorization", requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        // Approver tries -> 403
        mockMvc.perform(post("/admin/requests/1/override")
                        .header("Authorization", approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin Override: Missing remarks fails validation (400 Bad Request)")
    void testAdminOverrideMissingRemarks() throws Exception {
        AdminOverrideDto dto = new AdminOverrideDto(RequestStatus.APPROVED, "");

        mockMvc.perform(post("/admin/requests/1/override")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")));
    }
}
