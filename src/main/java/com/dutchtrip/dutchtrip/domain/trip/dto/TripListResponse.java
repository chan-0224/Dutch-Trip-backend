package com.dutchtrip.dutchtrip.domain.trip.dto;

import com.dutchtrip.dutchtrip.domain.trip.entity.Trip;
import com.dutchtrip.dutchtrip.domain.trip.entity.TripMemberRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class TripListResponse {

    private Long tripId;
    private String title;
    private String nation;
    private LocalDate startDate;
    private LocalDate endDate;
    private long memberCount;
    private String myRole;

    public static TripListResponse from(Trip trip, long memberCount, TripMemberRole role) {
        return new TripListResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getNation(),
                trip.getStartDate(),
                trip.getEndDate(),
                memberCount,
                role == TripMemberRole.OWNER ? "방장" : "일반"
        );
    }
}
