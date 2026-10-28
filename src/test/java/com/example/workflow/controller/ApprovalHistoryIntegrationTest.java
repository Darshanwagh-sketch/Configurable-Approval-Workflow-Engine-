package com.example.workflow.controller;

import com.example.workflow.dto.ApproveRequestDto;
import com.example.workflow.dto.CreateRequestDto;
import com.example.workflow.dto.RequestResponseDto;
import com.example.workflow.enums.HistoryAction;
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
public class ApprovalHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private String requester1Token;
    private String requester2Token;
    private String approver1Token;
    private String adminToken;

    @BeforeEach
    void setUp() {
        requester1Token = "Bearer " + jwtService.generateToken("requester1", Role.REQUESTER);
        requester2Token = "Bearer " + jwtService.generateToken("requester2", Role.REQUESTER);
        approver1Token = "Bearer " + jwtService.generateToken("approver1", Role.APPROVER);
        adminToken = "Bearer " + jwtService.generateToken("admin", Role.ADMIN);
    }

    @Test
    @DisplayName("Audit History: Chronological events recorded and visible to request owner")
    void testHistoryRetrievalByCreator() throws Exception {
        // Create request
        CreateRequestDto dto = new CreateRequestDto("LEAVE", Map.of("reason", "Family event"));
        MvcResult createRes = mockMvc.perform(post("/requests")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        RequestResponseDto req = objectMapper.readValue(createRes.getResponse().getContentAsString(), RequestResponseDto.class);
        Long id = req.getId();

        // Step 1 approval
        mockMvc.perform(post("/requests/" + id + "/approve")
                        .header("Authorization", approver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRequestDto("Approved step 1"))))
                .andExpect(status().isOk());

        // Step 2 approval by admin
        mockMvc.perform(post("/requests/" + id + "/approve")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRequestDto("Approved step 2 by admin"))))
                .andExpect(status().isOk());

        // Requester retrieves history
        mockMvc.perform(get("/requests/history/" + id)
                        .header("Authorization", requester1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].action", is("REQUEST_CREATED")))
                .andExpect(jsonPath("$[0].actionBy", is("requester1")))
                .andExpect(jsonPath("$[0].stepOrder", is(1)))
                .andExpect(jsonPath("$[1].action", is("APPROVED")))
                .andExpect(jsonPath("$[1].actionBy", is("approver1")))
                .andExpect(jsonPath("$[1].stepOrder", is(1)))
                .andExpect(jsonPath("$[1].remarks", is("Approved step 1")))
                .andExpect(jsonPath("$[2].action", is("APPROVED")))
                .andExpect(jsonPath("$[2].actionBy", is("admin")))
                .andExpect(jsonPath("$[2].stepOrder", is(2)))
                .andExpect(jsonPath("$[2].remarks", is("Approved step 2 by admin")));
    }

    @Test
    @DisplayName("Audit History: Unrelated requester is forbidden from accessing another user's history (403)")
    void testHistoryForbiddenForUnrelatedRequester() throws Exception {
        CreateRequestDto dto = new CreateRequestDto("EXPENSE", Map.of("amount", 200));
        MvcResult createRes = mockMvc.perform(post("/requests")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        RequestResponseDto req = objectMapper.readValue(createRes.getResponse().getContentAsString(), RequestResponseDto.class);

        mockMvc.perform(get("/requests/history/" + req.getId())
                        .header("Authorization", requester2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Forbidden Action")));
    }

    @Test
    @DisplayName("Audit History: Admin can view audit history for any user's request (200 OK)")
    void testAdminCanViewAnyRequestHistory() throws Exception {
        CreateRequestDto dto = new CreateRequestDto("EXPENSE", Map.of("amount", 300));
        MvcResult createRes = mockMvc.perform(post("/requests")
                        .header("Authorization", requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        RequestResponseDto req = objectMapper.readValue(createRes.getResponse().getContentAsString(), RequestResponseDto.class);

        mockMvc.perform(get("/requests/history/" + req.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Audit History: Non-existent request ID returns 404 Not Found")
    void testHistoryNonExistentRequest() throws Exception {
        mockMvc.perform(get("/requests/history/999999")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Resource Not Found")));
    }
}
