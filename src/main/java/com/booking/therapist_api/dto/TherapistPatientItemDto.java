package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TherapistPatientItemDto(
        UUID profileId,
        String patientName,
        String assignmentStatus,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant assignedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant unassignedAt,
        String riskLevel,
        List<String> tags
) {
}
