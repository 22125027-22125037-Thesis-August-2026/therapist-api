package com.booking.therapist_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ClinicalNoteRequestDto(
        @NotNull(message = "appointmentId is required")
        UUID appointmentId,

        @NotBlank(message = "diagnosis is required")
        String diagnosis,

        @NotBlank(message = "recommendations is required")
        String recommendations
) {
}
