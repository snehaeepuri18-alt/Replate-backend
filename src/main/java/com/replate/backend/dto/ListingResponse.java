package com.replate.backend.dto;

import com.replate.backend.enums.ListingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ListingResponse(
        Long id,
        Long restaurantId,
        String restaurantName,
        String foodName,
        String category,
        String description,
        BigDecimal originalPrice,
        BigDecimal discountedPrice,
        int quantity,
        LocalDateTime pickupDeadline,
        ListingStatus status,
        String imageUrl
) {}
