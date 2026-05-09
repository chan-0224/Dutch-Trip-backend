package com.dutchtrip.dutchtrip.domain.trip.controller;

import com.dutchtrip.dutchtrip.domain.trip.dto.JoinTripRequest;
import com.dutchtrip.dutchtrip.domain.trip.dto.TripCreateRequest;
import com.dutchtrip.dutchtrip.domain.trip.dto.TripResponse;
import com.dutchtrip.dutchtrip.domain.trip.service.TripService;
import com.dutchtrip.dutchtrip.global.common.ApiResponse;
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
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TripCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(tripService.createTrip(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TripResponse>>> getMyTrips(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getMyTrips(userId)));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTrip(userId, tripId)));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<TripResponse>> joinTrip(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody JoinTripRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.joinTrip(userId, request)));
    }
}
