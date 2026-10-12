package com.replate.backend.dto;

import com.replate.backend.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long restaurantId,
        String restaurantName,
        String customerName,
        String customerEmail,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime orderDate
) {}