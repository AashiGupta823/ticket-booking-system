package com.tbs.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class VenueDtos {

    public record CategoryInput(@NotBlank String name) {}

    public record SeatInput(String rowLabel, int seatCount, String categoryName) {}

    public record CreateVenueRequest(
            @NotBlank String name,
            @NotBlank String address,
            @NotBlank String city,
            List<CategoryInput> categories,
            List<SeatInput> rows // one entry per row, generates seatCount seats in that row
    ) {}
}
