package com.replate.backend.dto;

import com.replate.backend.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(
        @NotNull(message = "Status is required") OrderStatus status
) {}