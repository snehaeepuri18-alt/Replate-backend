package com.replate.backend.service;

import com.replate.backend.dto.*;
import com.replate.backend.entity.*;
import com.replate.backend.enums.ListingStatus;
import com.replate.backend.enums.OrderStatus;
import com.replate.backend.exception.ApiException;
import com.replate.backend.repository.FoodListingRepository;
import com.replate.backend.repository.OrderRepository;
import com.replate.backend.repository.RestaurantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class OrderService {

    // which status changes are allowed
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.CANCELLED),
            OrderStatus.READY_FOR_PICKUP, Set.of(OrderStatus.COMPLETED),
            OrderStatus.COMPLETED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    private final OrderRepository orderRepository;
    private final FoodListingRepository listingRepository;
    private final RestaurantRepository restaurantRepository;

    public OrderService(OrderRepository orderRepository,
                        FoodListingRepository listingRepository,
                        RestaurantRepository restaurantRepository) {
        this.orderRepository = orderRepository;
        this.listingRepository = listingRepository;
        this.restaurantRepository = restaurantRepository;
    }

    // ---------- customer ----------

    @Transactional
    public OrderResponse placeOrder(User customer, OrderRequest req) {

        // combine repeated items, sorted by id (a fixed order avoids database deadlocks)
        Map<Long, Integer> wanted = new TreeMap<>();
        for (OrderItemRequest item : req.items()) {
            wanted.merge(item.listingId(), item.quantity(), Integer::sum);
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setCustomer(customer);

        Restaurant restaurant = null;
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            int qty = entry.getValue();

            // 1. the listing must exist (and the row is locked while we check it)
            FoodListing listing = listingRepository.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "A food item in your cart no longer exists"));
            String name = listing.getFoodName();

            // 2. it must be active
            if (listing.getStatus() != ListingStatus.ACTIVE) {
                throw new ApiException(HttpStatus.CONFLICT, name + " is not available any more");
            }

            // 3. the pickup deadline must not have passed
            if (listing.getPickupDeadline().isBefore(now)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "The pickup time for " + name + " is over");
            }

            // 4. there must be enough quantity
            if (listing.getQuantity() < qty) {
                String left = listing.getQuantity() <= 0
                        ? name + " is sold out"
                        : "Only " + listing.getQuantity() + " of " + name + " left";
                throw new ApiException(HttpStatus.CONFLICT, left);
            }

            // one order = one restaurant
            if (restaurant == null) {
                restaurant = listing.getRestaurant();
            } else if (!restaurant.getId().equals(listing.getRestaurant().getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "One order can only contain food from one restaurant");
            }

            // reduce the stock
            listing.setQuantity(listing.getQuantity() - qty);
            if (listing.getQuantity() == 0) {
                listing.setStatus(ListingStatus.SOLD_OUT);
            }
            listingRepository.save(listing);

            // the price comes from the database, never from the frontend
            OrderItem item = new OrderItem();
            item.setFoodListing(listing);
            item.setQuantity(qty);
            item.setPrice(listing.getDiscountedPrice());
            order.addItem(item);

            total = total.add(listing.getDiscountedPrice().multiply(BigDecimal.valueOf(qty)));
        }

        order.setRestaurant(restaurant);
        order.setTotalAmount(total);
        order.setOrderStatus(OrderStatus.PENDING);

        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(User customer) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customer.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse cancelByCustomer(User customer, Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(this::notFound);

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw notFound();
        }
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Only pending orders can be cancelled");
        }

        cancelAndRestoreStock(order);
        return toResponse(orderRepository.save(order));
    }

    // ---------- restaurant ----------

    @Transactional(readOnly = true)
    public List<OrderResponse> restaurantOrders(User user) {
        Restaurant restaurant = myRestaurant(user);
        return orderRepository.findByRestaurantIdOrderByOrderDateDesc(restaurant.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse updateStatus(User user, Long orderId, OrderStatus newStatus) {
        Restaurant restaurant = myRestaurant(user);
        Order order = orderRepository.findById(orderId).orElseThrow(this::notFound);

        if (!order.getRestaurant().getId().equals(restaurant.getId())) {
            throw notFound();
        }

        OrderStatus current = order.getOrderStatus();
        if (!ALLOWED.get(current).contains(newStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "An order cannot go from " + current + " to " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            cancelAndRestoreStock(order);
        } else {
            order.setOrderStatus(newStatus);
        }
        return toResponse(orderRepository.save(order));
    }

    // ---------- helpers ----------

    // a cancelled order gives its food back to the listing
    private void cancelAndRestoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            FoodListing listing = listingRepository
                    .findByIdForUpdate(item.getFoodListing().getId())
                    .orElse(null);
            if (listing == null) continue;

            listing.setQuantity(listing.getQuantity() + item.getQuantity());
            if (listing.getStatus() == ListingStatus.SOLD_OUT) {
                listing.setStatus(ListingStatus.ACTIVE);
            }
            listingRepository.save(listing);
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
    }

    private Restaurant myRestaurant(User user) {
        return restaurantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Only restaurant accounts can manage orders"));
    }

    private OrderResponse toResponse(Order o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getFoodListing().getId(),
                        i.getFoodListing().getFoodName(),
                        i.getFoodListing().getCategory(),
                        i.getQuantity(),
                        i.getPrice()))
                .toList();

        return new OrderResponse(
                o.getId(),
                o.getRestaurant().getId(),
                o.getRestaurant().getRestaurantName(),
                o.getCustomer().getName(),
                o.getCustomer().getEmail(),
                items,
                o.getTotalAmount(),
                o.getOrderStatus(),
                o.getOrderDate());
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Order not found");
    }
}