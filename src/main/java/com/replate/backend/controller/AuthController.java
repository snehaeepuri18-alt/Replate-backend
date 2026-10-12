package com.replate.backend.controller;

import com.replate.backend.dto.AuthResponse;
import com.replate.backend.dto.LoginRequest;
import com.replate.backend.dto.RegisterRequest;
import com.replate.backend.dto.UserResponse;
import com.replate.backend.entity.User;
import com.replate.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // who am I? requires a valid token
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return authService.toUserResponse(user);
    }
}