package com.replate.backend.controller;

import com.replate.backend.dto.OrderResponse;
import com.replate.backend.dto.StatusRequest;
import com.replate.backend.entity.User;
import com.replate.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant/orders")
public class RestaurantOrderController {

    private final OrderService orderService;

    public RestaurantOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> incoming(@AuthenticationPrincipal User user) {
        return orderService.restaurantOrders(user);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@AuthenticationPrincipal User user,
                                      @PathVariable Long id,
                                      @Valid @RequestBody StatusRequest request) {
        return orderService.updateStatus(user, id, request.status());
    }
}