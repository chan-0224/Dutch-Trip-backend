package com.dutchtrip.dutchtrip.domain.schedule.dto;

import com.dutchtrip.dutchtrip.domain.schedule.entity.Schedule;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ScheduleResponse {

    private Long scheduleId;
    private LocalDate scheduleDate;
    private LocalDateTime scheduleTime;
    private String title;
    private String content;

    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getScheduledAt().toLocalDate(),
                schedule.getScheduledAt(),
                schedule.getTitle(),
                schedule.getContent()
        );
    }
}
