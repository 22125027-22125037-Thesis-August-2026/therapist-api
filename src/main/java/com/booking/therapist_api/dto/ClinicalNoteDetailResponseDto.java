package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClinicalNoteDetailResponseDto(
        UUID noteId,
        UUID appointmentId,
        UUID profileId,
        UUID therapistId,
        String appointmentStatus,
        String status,

        String diagnosis,
        String recommendations,
        String subjective,
        String objective,
        String assessment,
        String plan,
        String summary,

        RiskFlags riskFlags,

        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant updatedAt
) {
    public record RiskFlags(
            boolean suicidalIdeation,
            boolean selfHarm,
            boolean substanceUse,
            boolean abuse
    ) {
    }
}
