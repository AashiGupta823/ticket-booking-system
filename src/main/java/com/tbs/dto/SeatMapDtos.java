package com.tbs.dto;

public class SeatMapDtos {

    public record SeatStatusDto(
            Long showSeatId,
            Long seatId,
            String rowLabel,
            int seatNumber,
            String category,
            Long categoryId,
            String status // AVAILABLE / HELD / BOOKED
    ) {}

    public record HoldRequest(Long showSeatId) {}

    public record ConfirmBookingRequest(java.util.List<Long> showSeatIds) {}
}
