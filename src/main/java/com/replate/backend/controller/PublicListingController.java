package com.replate.backend.controller;

import com.replate.backend.dto.ListingResponse;
import com.replate.backend.service.ListingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class PublicListingController {

    private final ListingService listingService;

    public PublicListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public List<ListingResponse> all() {
        return listingService.getPublicListings();
    }

    @GetMapping("/{id}")
    public ListingResponse one(@PathVariable Long id) {
        return listingService.getPublicListing(id);
    }
}