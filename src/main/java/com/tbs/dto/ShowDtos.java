package com.tbs.dto;

import com.tbs.entity.Show;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ShowDtos {

    public record CategoryPriceInput(@NotBlank String categoryName, @NotNull BigDecimal price) {}

    public record CreateShowRequest(
            @NotBlank String title,
            @NotNull Show.ShowType type,
            @NotNull Long venueId,
            @NotNull Instant startTime,
            String description,
            List<CategoryPriceInput> prices
    ) {}
}
