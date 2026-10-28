package com.example.workflow.service;

import com.example.workflow.dto.AuthRequest;
import com.example.workflow.dto.AuthResponse;
import com.example.workflow.dto.RegisterRequest;

public interface AuthenticationService {
    AuthResponse login(AuthRequest request);
    AuthResponse register(RegisterRequest request);
}
