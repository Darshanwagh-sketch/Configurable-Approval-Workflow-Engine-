package com.example.workflow.security;

import com.example.workflow.enums.Role;
import com.example.workflow.exception.UnauthorizedActionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("SecurityUtils: Extracts username and checks role from UserDetails principal")
    void testExtractUserDetails() {
        User user = new User("admin_user", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals("admin_user", SecurityUtils.getCurrentUsername());
        assertTrue(SecurityUtils.hasRole(Role.ADMIN));
        assertFalse(SecurityUtils.hasRole(Role.REQUESTER));
    }

    @Test
    @DisplayName("SecurityUtils: Extracts username from String principal")
    void testExtractStringPrincipal() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("string_user", null, List.of(new SimpleGrantedAuthority("ROLE_REQUESTER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals("string_user", SecurityUtils.getCurrentUsername());
        assertTrue(SecurityUtils.hasRole(Role.REQUESTER));
    }

    @Test
    @DisplayName("SecurityUtils: Throws UnauthorizedActionException when unauthenticated")
    void testUnauthenticatedThrows() {
        SecurityContextHolder.clearContext();

        assertThrows(UnauthorizedActionException.class, SecurityUtils::getCurrentUsername);
        assertFalse(SecurityUtils.hasRole(Role.ADMIN));
    }
}
