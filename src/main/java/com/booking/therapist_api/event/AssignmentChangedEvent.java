package com.booking.therapist_api.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted whenever a patient is assigned to or unassigned from a therapist.
 * {@code occurredAt} is the moment of the state change and is used downstream
 * for ordering. {@code status} is either {@code ACTIVE} or {@code INACTIVE}.
 */
public record AssignmentChangedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID therapistProfileId,
        UUID patientProfileId,
        String status,
        Instant assignedAt
) {
}
