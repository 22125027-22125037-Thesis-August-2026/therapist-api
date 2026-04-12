package com.booking.therapist_api.event;

import java.time.Instant;
import java.util.UUID;

public record CrisisAlertEvent(UUID profileId, String source, String triggerReason, Instant timestamp) {
}
