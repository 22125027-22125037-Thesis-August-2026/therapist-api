package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TherapistAppointmentItemDto(
        UUID appointmentId,
        UUID profileId,
        String patientName,
        UUID therapistId,
        UUID slotId,
        String mode,
        String status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant startDatetime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant endDatetime,
        String reason
) {
}
