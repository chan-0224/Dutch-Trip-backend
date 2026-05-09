package com.dutchtrip.dutchtrip.domain.trip.dto;

import com.dutchtrip.dutchtrip.domain.trip.entity.Trip;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TripResponse {

    private Long id;
    private String title;
    private String nation;
    private LocalDate startDate;
    private LocalDate endDate;
    private String inviteCode;
    private LocalDateTime createdAt;

    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getNation(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getInviteCode(),
                trip.getCreatedAt()
        );
    }
}
