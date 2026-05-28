package com.booking.therapist_api.dto;

import com.booking.therapist_api.enums.AppointmentMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BookingRequestDto(
        @NotNull UUID slotId,
        @Size(max = 1000, message = "reason must not exceed 1000 characters") String reason,
        AppointmentMode mode
) {
}
