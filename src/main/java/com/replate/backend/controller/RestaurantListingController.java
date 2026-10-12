package com.replate.backend.controller;

import com.replate.backend.dto.ListingRequest;
import com.replate.backend.dto.ListingResponse;
import com.replate.backend.entity.User;
import com.replate.backend.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant/listings")
public class RestaurantListingController {

    private final ListingService listingService;

    public RestaurantListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public List<ListingResponse> mine(@AuthenticationPrincipal User user) {
        return listingService.getMyListings(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListingResponse create(@AuthenticationPrincipal User user,
                                  @Valid @RequestBody ListingRequest request) {
        return listingService.create(user, request);
    }

    @PutMapping("/{id}")
    public ListingResponse update(@AuthenticationPrincipal User user,
                                  @PathVariable Long id,
                                  @Valid @RequestBody ListingRequest request) {
        return listingService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        listingService.delete(user, id);
    }

    @PatchMapping("/{id}/toggle")
    public ListingResponse toggle(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return listingService.toggle(user, id);
    }
}
