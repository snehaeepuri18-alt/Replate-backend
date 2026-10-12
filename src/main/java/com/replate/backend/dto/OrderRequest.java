package com.replate.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "Your cart is empty") @Valid List<OrderItemRequest> items
) {}