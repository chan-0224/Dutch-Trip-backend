package com.dutchtrip.dutchtrip.domain.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
public class TripCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String nation;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private List<FixedCostRequest> fixedCosts;

    @Getter
    public static class FixedCostRequest {
        @NotBlank
        private String title;

        @NotNull
        private BigDecimal totalAmount;
    }
}
