package com.replate.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "Food item is required") Long listingId,
        @Min(value = 1, message = "Quantity must be at least 1") int quantity
) {}