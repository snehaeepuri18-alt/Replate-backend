package com.replate.backend.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long listingId,
        String foodName,
        String category,
        int quantity,
        BigDecimal price
) {}