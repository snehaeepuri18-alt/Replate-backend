package com.replate.backend.scheduler;

import com.replate.backend.enums.ListingStatus;
import com.replate.backend.repository.FoodListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ListingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ListingExpiryJob.class);

    private final FoodListingRepository listingRepository;

    public ListingExpiryJob(FoodListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    // runs every minute: ACTIVE listings whose deadline has passed become EXPIRED
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireListings() {
        int count = listingRepository.markExpired(
                ListingStatus.ACTIVE, ListingStatus.EXPIRED, LocalDateTime.now());
        if (count > 0) {
            log.info("Expired {} listing(s)", count);
        }
    }
}