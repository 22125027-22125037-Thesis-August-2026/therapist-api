package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant startDatetime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant endDatetime,
        String reason
) {
}
