package com.dutchtrip.dutchtrip.domain.trip.controller;

import com.dutchtrip.dutchtrip.domain.trip.dto.JoinTripRequest;
import com.dutchtrip.dutchtrip.domain.trip.dto.TripCreateRequest;
import com.dutchtrip.dutchtrip.domain.trip.dto.TripCreateResponse;
import com.dutchtrip.dutchtrip.domain.trip.dto.TripListResponse;
import com.dutchtrip.dutchtrip.domain.trip.dto.TripMemberResponse;
import com.dutchtrip.dutchtrip.domain.trip.dto.TripResponse;
import com.dutchtrip.dutchtrip.domain.trip.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripCreateResponse> createTrip(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TripCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tripService.createTrip(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<TripListResponse>> getMyTrips(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(tripService.getMyTrips(userId));
    }

    @GetMapping("/{tripId}/members")
    public ResponseEntity<List<TripMemberResponse>> getTripMembers(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long tripId) {
        return ResponseEntity.ok(tripService.getTripMembers(userId, tripId));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<TripResponse> getTrip(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long tripId) {
        return ResponseEntity.ok(tripService.getTrip(userId, tripId));
    }

    @PostMapping("/join")
    public ResponseEntity<TripResponse> joinTrip(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody JoinTripRequest request) {
        return ResponseEntity.ok(tripService.joinTrip(userId, request));
    }
}
