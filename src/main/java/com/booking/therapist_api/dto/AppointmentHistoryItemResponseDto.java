package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

public record AppointmentHistoryItemResponseDto(
        UUID appointmentId,
        UUID profileId,
        UUID therapistId,
        String therapistName,
        String therapistSpecialization,
        String location,
        UUID slotId,
        String mode,
        String status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant startDatetime
) {
}
