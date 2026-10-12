package com.replate.backend.controller;

import com.replate.backend.dto.OrderRequest;
import com.replate.backend.dto.OrderResponse;
import com.replate.backend.entity.User;
import com.replate.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@AuthenticationPrincipal User user,
                               @Valid @RequestBody OrderRequest request) {
        return orderService.placeOrder(user, request);
    }

    @GetMapping
    public List<OrderResponse> mine(@AuthenticationPrincipal User user) {
        return orderService.myOrders(user);
    }

    @PatchMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return orderService.cancelByCustomer(user, id);
    }
}