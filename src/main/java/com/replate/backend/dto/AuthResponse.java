package com.replate.backend.dto;

public record AuthResponse(String token, UserResponse user) {}