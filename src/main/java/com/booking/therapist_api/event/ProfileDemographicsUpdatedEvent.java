package com.booking.therapist_api.event;

import java.util.UUID;

public record ProfileDemographicsUpdatedEvent(UUID profileId, Integer age, String gender) {
}
