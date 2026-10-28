package com.example.workflow.controller;

import com.example.workflow.dto.AuthRequest;
import com.example.workflow.dto.RegisterRequest;
import com.example.workflow.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /auth/login - Success with valid seeded credentials")
    void testLoginSuccess() throws Exception {
        AuthRequest request = new AuthRequest("requester1", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.username", is("requester1")))
                .andExpect(jsonPath("$.role", is("REQUESTER")))
                .andExpect(jsonPath("$.expiresInMs", greaterThan(0)));
    }

    @Test
    @DisplayName("POST /auth/login - Failure with wrong password returns 401")
    void testLoginWrongPassword() throws Exception {
        AuthRequest request = new AuthRequest("requester1", "wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Authentication Failed")));
    }

    @Test
    @DisplayName("POST /auth/login - Failure with non-existent user returns 401")
    void testLoginNonExistentUser() throws Exception {
        AuthRequest request = new AuthRequest("nonexistentuser", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Authentication Failed")));
    }

    @Test
    @DisplayName("POST /auth/login - Blank credentials return 400 Bad Request")
    void testLoginBlankValidation() throws Exception {
        AuthRequest request = new AuthRequest("", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")));
    }

    @Test
    @DisplayName("POST /auth/register - Success creating new REQUESTER")
    void testRegisterRequesterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("newrequester", "password123", Role.REQUESTER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("newrequester")))
                .andExpect(jsonPath("$.role", is("REQUESTER")));
    }

    @Test
    @DisplayName("POST /auth/register - Success creating new APPROVER")
    void testRegisterApproverSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("newapprover", "password123", Role.APPROVER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("newapprover")))
                .andExpect(jsonPath("$.role", is("APPROVER")));
    }

    @Test
    @DisplayName("POST /auth/register - Privilege escalation to ADMIN is strictly forbidden (403)")
    void testRegisterAdminForbidden() throws Exception {
        RegisterRequest request = new RegisterRequest("sneakyadmin", "password123", Role.ADMIN);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden Action")));
    }

    @Test
    @DisplayName("POST /auth/register - Duplicate username returns 409 Conflict")
    void testRegisterDuplicateUsername() throws Exception {
        RegisterRequest request = new RegisterRequest("requester1", "password123", Role.REQUESTER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("User Conflict")));
    }
}
