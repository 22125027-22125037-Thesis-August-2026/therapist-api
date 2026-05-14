package com.booking.therapist_api.dto;

import java.time.Instant;
import java.util.UUID;

public record ClinicalNoteDetailResponseDto(
        UUID noteId,
        UUID appointmentId,
        String appointmentStatus,
        String diagnosis,
        String recommendations,
        Instant createdAt
) {
}
