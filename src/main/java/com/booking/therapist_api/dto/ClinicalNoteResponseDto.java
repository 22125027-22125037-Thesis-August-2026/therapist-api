package com.booking.therapist_api.dto;

import java.time.Instant;
import java.util.UUID;

public record ClinicalNoteResponseDto(
        UUID noteId,
        UUID appointmentId,
        String appointmentStatus,
        String status,
        Instant createdAt,
        String message
) {
}
