package com.replate.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ListingRequest(
        @NotBlank(message = "Enter the food name") String foodName,
        @NotBlank(message = "Choose a category") String category,
        String description,
        @NotNull(message = "Enter the original price")
        @DecimalMin(value = "0.01", message = "Original price must be more than 0") BigDecimal originalPrice,
        @NotNull(message = "Enter the discounted price")
        @DecimalMin(value = "0.01", message = "Discounted price must be more than 0") BigDecimal discountedPrice,
        @Min(value = 1, message = "Quantity must be at least 1") int quantity,
        @NotNull(message = "Choose a pickup deadline") LocalDateTime pickupDeadline,
        String imageUrl
) {}