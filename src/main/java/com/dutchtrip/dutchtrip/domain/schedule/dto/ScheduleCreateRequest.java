package com.dutchtrip.dutchtrip.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ScheduleCreateRequest {

    @NotNull
    private LocalDateTime scheduledAt;

    @NotBlank
    private String title;

    private String content;
}
