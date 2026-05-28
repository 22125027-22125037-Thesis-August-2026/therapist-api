package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TherapistSlotResponseDto(
        UUID slotId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant startDatetime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant endDatetime,
        boolean isBooked,
        UUID bookedByPatientId,
        String bookedByPatientName,
        UUID appointmentId
) {
}
