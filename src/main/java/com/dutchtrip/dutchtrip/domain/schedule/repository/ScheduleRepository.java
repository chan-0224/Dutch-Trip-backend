package com.dutchtrip.dutchtrip.domain.schedule.repository;

import com.dutchtrip.dutchtrip.domain.schedule.entity.Schedule;
import com.dutchtrip.dutchtrip.domain.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findAllByTripOrderByScheduledAtAsc(Trip trip);
    List<Schedule> findAllByTripAndScheduledAtBetweenOrderByScheduledAtAsc(Trip trip, LocalDateTime start, LocalDateTime end);
}
