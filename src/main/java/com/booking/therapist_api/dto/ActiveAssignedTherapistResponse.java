package com.booking.therapist_api.dto;

import java.time.Instant;
import java.util.UUID;

public record ActiveAssignedTherapistResponse(
        UUID assignmentId,
        UUID profileId,
        String status,
        Instant assignedAt,
        AssignedTherapistSummary therapist
) {
}
