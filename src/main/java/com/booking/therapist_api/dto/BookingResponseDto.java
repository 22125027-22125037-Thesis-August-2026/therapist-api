package com.booking.therapist_api.dto;

import java.util.UUID;

public record BookingResponseDto(UUID appointmentId, UUID slotId, String status, String message) {
}
