package com.example.workflow.service.impl;

import com.example.workflow.dto.AuthRequest;
import com.example.workflow.dto.AuthResponse;
import com.example.workflow.dto.RegisterRequest;
import com.example.workflow.entity.User;
import com.example.workflow.enums.Role;
import com.example.workflow.exception.UnauthorizedActionException;
import com.example.workflow.exception.UserAlreadyExistsException;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.security.JwtService;
import com.example.workflow.service.AuthenticationService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getUsername()));

        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getRole(), jwtService.getExpirationMs());
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == Role.ADMIN) {
            throw new UnauthorizedActionException("Registration for the ADMIN role is strictly prohibited. Administrative accounts must be provisioned through secure configuration.");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken.");
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.REQUESTER;

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                assignedRole
        );

        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getRole(), jwtService.getExpirationMs());
    }
}
