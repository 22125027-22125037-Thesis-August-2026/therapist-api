package com.booking.therapist_api.event;

import java.time.Instant;
import java.util.UUID;

public record AppointmentBookedEvent(UUID appointmentId, Instant timestamp) {
}
