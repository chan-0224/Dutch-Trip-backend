package com.dutchtrip.dutchtrip.domain.trip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class JoinTripRequest {

    @NotBlank
    private String inviteCode;
}
