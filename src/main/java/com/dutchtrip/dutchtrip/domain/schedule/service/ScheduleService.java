package com.dutchtrip.dutchtrip.domain.schedule.service;

import com.dutchtrip.dutchtrip.domain.schedule.dto.ScheduleCreateRequest;
import com.dutchtrip.dutchtrip.domain.schedule.dto.ScheduleResponse;
import com.dutchtrip.dutchtrip.domain.schedule.entity.Schedule;
import com.dutchtrip.dutchtrip.domain.schedule.repository.ScheduleRepository;
import com.dutchtrip.dutchtrip.domain.trip.entity.Trip;
import com.dutchtrip.dutchtrip.domain.trip.repository.TripMemberRepository;
import com.dutchtrip.dutchtrip.domain.trip.repository.TripRepository;
import com.dutchtrip.dutchtrip.domain.user.entity.User;
import com.dutchtrip.dutchtrip.domain.user.repository.UserRepository;
import com.dutchtrip.dutchtrip.global.exception.CustomException;
import com.dutchtrip.dutchtrip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ScheduleResponse createSchedule(Long userId, Long tripId, ScheduleCreateRequest request) {
        User user = findUser(userId);
        Trip trip = findTrip(tripId);
        checkMembership(trip, user);

        Schedule schedule = Schedule.builder()
                .trip(trip)
                .scheduledAt(request.getScheduledAt())
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    public List<ScheduleResponse> getSchedules(Long userId, Long tripId, LocalDate date) {
        User user = findUser(userId);
        Trip trip = findTrip(tripId);
        checkMembership(trip, user);

        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(23, 59, 59);
            return scheduleRepository
                    .findAllByTripAndScheduledAtBetweenOrderByScheduledAtAsc(trip, start, end)
                    .stream().map(ScheduleResponse::from).toList();
        }

        return scheduleRepository.findAllByTripOrderByScheduledAtAsc(trip)
                .stream().map(ScheduleResponse::from).toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Trip findTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_FOUND));
    }

    private void checkMembership(Trip trip, User user) {
        if (!tripMemberRepository.existsByTripAndUser(trip, user)) {
            throw new CustomException(ErrorCode.NOT_TRIP_MEMBER);
        }
    }
}
