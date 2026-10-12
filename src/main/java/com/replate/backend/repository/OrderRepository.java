package com.replate.backend.repository;

import com.replate.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    List<Order> findByRestaurantIdOrderByOrderDateDesc(Long restaurantId);
}