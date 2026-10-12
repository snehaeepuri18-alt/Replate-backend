package com.replate.backend.repository;

import com.replate.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByFoodListingId(Long foodListingId);
}