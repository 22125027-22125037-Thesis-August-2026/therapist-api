package com.booking.therapist_api.event;

import java.time.Instant;
import java.util.UUID;

public record AppointmentBookedEvent(
	UUID messageId,
	Instant occurredAt,
	UUID appointmentId,
	UUID profileId,
	String userEmail,
	String userName,
	String therapistName,
	Instant startTime
) {
}
