package com.dutchtrip.dutchtrip.domain.trip.dto;

import com.dutchtrip.dutchtrip.domain.trip.entity.Trip;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TripCreateResponse {

    private Long tripId;
    private String title;
    private String inviteCode;

    public static TripCreateResponse from(Trip trip) {
        return new TripCreateResponse(trip.getId(), trip.getTitle(), trip.getInviteCode());
    }
}
