package com.replate.backend.dto;

import com.replate.backend.enums.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        String restaurantName
) {}