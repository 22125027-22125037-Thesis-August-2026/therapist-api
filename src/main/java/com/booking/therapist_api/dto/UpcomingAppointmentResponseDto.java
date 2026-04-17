package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

public record UpcomingAppointmentResponseDto(
        UUID appointmentId,
        UUID profileId,
        UUID therapistId,
        UUID slotId,
        String mode,
        String status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant startDatetime
) {
}
