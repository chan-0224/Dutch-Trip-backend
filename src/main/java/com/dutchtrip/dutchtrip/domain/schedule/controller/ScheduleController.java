package com.dutchtrip.dutchtrip.domain.schedule.controller;

import com.dutchtrip.dutchtrip.domain.schedule.dto.ScheduleCreateRequest;
import com.dutchtrip.dutchtrip.domain.schedule.dto.ScheduleResponse;
import com.dutchtrip.dutchtrip.domain.schedule.service.ScheduleService;
import com.dutchtrip.dutchtrip.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long tripId,
            @Valid @RequestBody ScheduleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(scheduleService.createSchedule(userId, tripId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedules(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long tripId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.getSchedules(userId, tripId, date)));
    }
}
