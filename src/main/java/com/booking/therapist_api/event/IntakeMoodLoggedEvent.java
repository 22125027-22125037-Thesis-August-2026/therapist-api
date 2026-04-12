package com.booking.therapist_api.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IntakeMoodLoggedEvent(UUID profileId, Map<String, Integer> moodLevels, Instant timestamp) {
}
