package com.booking.therapist_api.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record WeeklyTemplateResponseDto(
        UUID templateId,
        UUID therapistId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean isActive
) {
}
