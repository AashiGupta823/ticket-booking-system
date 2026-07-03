package com.tbs.controller;

import com.tbs.dto.ShowDtos.*;
import com.tbs.entity.*;
import com.tbs.exception.ResourceNotFoundException;
import com.tbs.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Organiser creates movie/concert listings with venue, date, time, per-category pricing.
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowRepository showRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final SeatCategoryRepository seatCategoryRepository;
    private final ShowCategoryPriceRepository priceRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;

    @GetMapping
    public List<Show> browseShows(@RequestParam(required = false) String search,
                                   @RequestParam(required = false) Show.ShowType type) {
        List<Show> shows = (search == null || search.isBlank())
                ? showRepository.findAll()
                : showRepository.findByTitleContainingIgnoreCase(search);
        if (type != null) {
            shows = shows.stream().filter(s -> s.getType() == type).toList();
        }
        return shows;
    }

    @GetMapping("/{id}")
    public Show getShow(@PathVariable Long id) {
        return showRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Show not found"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Show> createShow(@Valid @RequestBody CreateShowRequest req,
                                            @AuthenticationPrincipal User organiser) {
        Venue venue = venueRepository.findById(req.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        Show show = Show.builder()
                .title(req.title())
                .type(req.type())
                .venue(venue)
                .organiser(organiser)
                .startTime(req.startTime())
                .description(req.description())
                .build();
        show = showRepository.save(show);

        for (CategoryPriceInput cp : req.prices()) {
            SeatCategory category = seatCategoryRepository.findByVenueId(venue.getId()).stream()
                    .filter(c -> c.getName().equalsIgnoreCase(cp.categoryName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + cp.categoryName()));
            priceRepository.save(ShowCategoryPrice.builder()
                    .show(show).category(category).price(cp.price()).build());
        }

        // Materialize one ShowSeat (status=AVAILABLE) per physical seat in the venue.
        // This is the row concurrency control locks on during hold/booking.
        for (Seat seat : seatRepository.findByVenueId(venue.getId())) {
            showSeatRepository.save(ShowSeat.builder()
                    .show(show).seat(seat).status(ShowSeat.Status.AVAILABLE).build());
        }

        return ResponseEntity.ok(show);
    }

    // Organiser: booking summary and revenue per event.
    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public java.util.Map<String, Object> summary(@PathVariable Long id) {
        List<Booking> bookings = bookingRepository.findByShowId(id).stream()
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED)
                .toList();
        java.math.BigDecimal revenue = bookings.stream()
                .map(Booking::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        long seatsSold = bookings.stream().mapToLong(b -> b.getSeats().size()).sum();
        return java.util.Map.of(
                "showId", id,
                "confirmedBookings", bookings.size(),
                "seatsSold", seatsSold,
                "revenue", revenue
        );
    }
}
