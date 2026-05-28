package com.booking.therapist_api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WeeklyTemplateRequestDto(
        @NotNull(message = "dayOfWeek is required") DayOfWeek dayOfWeek,
        @NotNull(message = "startTime is required") LocalTime startTime,
        @NotNull(message = "endTime is required") LocalTime endTime,
        Boolean isActive
) {
}
