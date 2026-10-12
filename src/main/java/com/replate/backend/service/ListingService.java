package com.replate.backend.service;

import com.replate.backend.dto.ListingRequest;
import com.replate.backend.dto.ListingResponse;
import com.replate.backend.entity.FoodListing;
import com.replate.backend.entity.Restaurant;
import com.replate.backend.entity.User;
import com.replate.backend.enums.ListingStatus;
import com.replate.backend.exception.ApiException;
import com.replate.backend.repository.FoodListingRepository;
import com.replate.backend.repository.OrderItemRepository;
import com.replate.backend.repository.RestaurantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ListingService {

    private final FoodListingRepository listingRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderItemRepository orderItemRepository;

    public ListingService(FoodListingRepository listingRepository,
                          RestaurantRepository restaurantRepository,
                          OrderItemRepository orderItemRepository) {
        this.listingRepository = listingRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // ---------- public (customers) ----------

    public List<ListingResponse> getPublicListings() {
        return listingRepository
                .findByStatusAndQuantityGreaterThanAndPickupDeadlineAfterOrderByCreatedAtDesc(
                        ListingStatus.ACTIVE, 0, LocalDateTime.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ListingResponse getPublicListing(Long id) {
        FoodListing listing = listingRepository.findById(id)
                .orElseThrow(() -> notFound());
        if (effectiveStatus(listing) != ListingStatus.ACTIVE) {
            throw new ApiException(HttpStatus.NOT_FOUND, "This food is not available");
        }
        return toResponse(listing);
    }

    // ---------- restaurant ----------

    public List<ListingResponse> getMyListings(User user) {
        Restaurant restaurant = myRestaurant(user);
        return listingRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurant.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ListingResponse create(User user, ListingRequest req) {
        validate(req);
        Restaurant restaurant = myRestaurant(user);

        FoodListing listing = new FoodListing();
        listing.setRestaurant(restaurant);
        copyFields(listing, req);
        listing.setStatus(ListingStatus.ACTIVE);

        return toResponse(listingRepository.save(listing));
    }

    // PUT
    @Transactional
    public ListingResponse update(User user, Long id, ListingRequest req) {
        validate(req);
        FoodListing listing = ownedListing(user, id);

        copyFields(listing, req);
        syncStatus(listing);

        return toResponse(listingRepository.save(listing));
    }

    // DELETE
    @Transactional
    public void delete(User user, Long id) {
        FoodListing listing = ownedListing(user, id);

        // a listing that already has orders must stay, or the order history would break
        if (orderItemRepository.existsByFoodListingId(listing.getId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This listing has orders, so it can't be deleted. Deactivate it instead.");
        }
        listingRepository.delete(listing);
    }

    // PATCH
    @Transactional
    public ListingResponse toggle(User user, Long id) {
        FoodListing listing = ownedListing(user, id);

        if (listing.getStatus() == ListingStatus.INACTIVE) {
            listing.setStatus(ListingStatus.ACTIVE);
            syncStatus(listing);
        } else {
            listing.setStatus(ListingStatus.INACTIVE);
        }
        return toResponse(listingRepository.save(listing));
    }

    // ---------- helpers ----------

    private Restaurant myRestaurant(User user) {
        return restaurantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Only restaurant accounts can manage listings"));
    }

    // a restaurant can only reach its own listings
    private FoodListing ownedListing(User user, Long id) {
        Restaurant restaurant = myRestaurant(user);
        FoodListing listing = listingRepository.findById(id).orElseThrow(() -> notFound());
        if (!listing.getRestaurant().getId().equals(restaurant.getId())) {
            throw notFound();
        }
        return listing;
    }

    private void validate(ListingRequest req) {
        if (req.discountedPrice().compareTo(req.originalPrice()) >= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Discounted price must be lower than the original price");
        }
        if (!req.pickupDeadline().isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Pickup deadline must be in the future");
        }
    }

    private void copyFields(FoodListing listing, ListingRequest req) {
        listing.setFoodName(req.foodName().trim());
        listing.setCategory(req.category());
        listing.setDescription(req.description() == null ? "" : req.description().trim());
        listing.setOriginalPrice(req.originalPrice());
        listing.setDiscountedPrice(req.discountedPrice());
        listing.setQuantity(req.quantity());
        listing.setPickupDeadline(req.pickupDeadline());
        listing.setImageUrl(req.imageUrl());
    }

    // keeps SOLD_OUT and ACTIVE matching the quantity (INACTIVE stays as it is)
    private void syncStatus(FoodListing listing) {
        if (listing.getStatus() == ListingStatus.INACTIVE) return;
        listing.setStatus(listing.getQuantity() <= 0 ? ListingStatus.SOLD_OUT : ListingStatus.ACTIVE);
    }

    // the real status right now, including a passed deadline
    private ListingStatus effectiveStatus(FoodListing l) {
        if (l.getStatus() == ListingStatus.INACTIVE) return ListingStatus.INACTIVE;
        if (l.getQuantity() <= 0) return ListingStatus.SOLD_OUT;
        if (l.getPickupDeadline().isBefore(LocalDateTime.now())) return ListingStatus.EXPIRED;
        return l.getStatus();
    }

    private ListingResponse toResponse(FoodListing l) {
        return new ListingResponse(
                l.getId(),
                l.getRestaurant().getId(),
                l.getRestaurant().getRestaurantName(),
                l.getFoodName(),
                l.getCategory(),
                l.getDescription(),
                l.getOriginalPrice(),
                l.getDiscountedPrice(),
                l.getQuantity(),
                l.getPickupDeadline(),
                effectiveStatus(l),
                l.getImageUrl());
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Listing not found");
    }
}