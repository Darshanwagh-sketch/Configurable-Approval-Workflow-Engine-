package com.example.workflow.exception;

import com.example.workflow.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Exception Handler: Maps AccessDeniedException to 403 Forbidden")
    void testAccessDeniedExceptionMapping() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/requests/99");
        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(
                new AccessDeniedException("Access denied"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Access Denied", response.getBody().getError());
        assertEquals("/requests/99", response.getBody().getPath());
    }

    @Test
    @DisplayName("Exception Handler: Maps BadCredentialsException to 401 Unauthorized")
    void testAuthenticationExceptionMapping() {
        HttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(
                new BadCredentialsException("Bad credentials"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Authentication Failed", response.getBody().getError());
    }

    @Test
    @DisplayName("Exception Handler: Maps IllegalArgumentException to 400 Bad Request")
    void testIllegalArgumentExceptionMapping() {
        HttpServletRequest request = new MockHttpServletRequest("POST", "/requests");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Illegal argument provided"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
    }

    @Test
    @DisplayName("Exception Handler: Maps unhandled Exception to 500 Internal Server Error without leaking details")
    void testGeneralExceptionMapping() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        ResponseEntity<ErrorResponse> response = handler.handleGeneralException(
                new NullPointerException("Internal crash"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertFalse(response.getBody().getMessage().contains("NullPointerException"), "Stack trace or internal details must not leak");
    }
}
