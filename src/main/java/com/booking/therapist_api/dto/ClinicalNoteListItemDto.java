package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClinicalNoteListItemDto(
        UUID noteId,
        UUID appointmentId,
        UUID profileId,
        String patientName,
        UUID therapistId,
        String status,
        String summary,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant appointmentStartDatetime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant updatedAt
) {
}
