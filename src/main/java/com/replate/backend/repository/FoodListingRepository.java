package com.replate.backend.repository;

import com.replate.backend.entity.FoodListing;
import com.replate.backend.enums.ListingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FoodListingRepository extends JpaRepository<FoodListing, Long> {

    List<FoodListing> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

    List<FoodListing> findByStatusAndQuantityGreaterThanAndPickupDeadlineAfterOrderByCreatedAtDesc(
            ListingStatus status, int quantity, LocalDateTime now);

    // locks the row until the order finishes, so stock can never go wrong
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from FoodListing l where l.id = :id")
    Optional<FoodListing> findByIdForUpdate(@Param("id") Long id);

    // used by the expiry job
    @Modifying
    @Query("update FoodListing l set l.status = :to where l.status = :from and l.pickupDeadline < :now")
    int markExpired(@Param("from") ListingStatus from,
                    @Param("to") ListingStatus to,
                    @Param("now") LocalDateTime now);
}