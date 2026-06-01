package com.booking.therapist_api.dto;

import java.time.Instant;
import java.util.UUID;

public record InternalAssignmentDto(
        UUID therapistProfileId,
        UUID patientProfileId,
        String status,
        Instant assignedAt,
        Instant updatedAt
) {
}
