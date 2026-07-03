package com.tbs.controller;

import com.tbs.dto.VenueDtos.*;
import com.tbs.entity.*;
import com.tbs.repository.SeatCategoryRepository;
import com.tbs.repository.SeatRepository;
import com.tbs.repository.VenueRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Admin creates and manages venues with seat layout and categories.
@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueRepository venueRepository;
    private final SeatCategoryRepository seatCategoryRepository;
    private final SeatRepository seatRepository;

    @GetMapping
    public List<Venue> listVenues() {
        return venueRepository.findAll();
    }

    @GetMapping("/{id}")
    public Venue getVenue(@PathVariable Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new com.tbs.exception.ResourceNotFoundException("Venue not found"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Venue> createVenue(@Valid @RequestBody CreateVenueRequest req,
                                              @org.springframework.security.core.annotation.AuthenticationPrincipal User admin) {
        Venue venue = Venue.builder()
                .name(req.name())
                .address(req.address())
                .city(req.city())
                .createdBy(admin)
                .build();
        venue = venueRepository.save(venue);

        Map<String, SeatCategory> categoriesByName = new HashMap<>();
        for (CategoryInput c : req.categories()) {
            SeatCategory cat = seatCategoryRepository.save(
                    SeatCategory.builder().venue(venue).name(c.name()).build());
            categoriesByName.put(c.name(), cat);
        }

        for (SeatInput row : req.rows()) {
            SeatCategory category = categoriesByName.get(row.categoryName());
            if (category == null) {
                throw new IllegalArgumentException("Unknown category: " + row.categoryName());
            }
            for (int n = 1; n <= row.seatCount(); n++) {
                seatRepository.save(Seat.builder()
                        .venue(venue)
                        .category(category)
                        .rowLabel(row.rowLabel())
                        .seatNumber(n)
                        .build());
            }
        }

        return ResponseEntity.ok(venue);
    }

    @GetMapping("/{id}/seats")
    public List<Seat> getSeats(@PathVariable Long id) {
        return seatRepository.findByVenueId(id);
    }
}
