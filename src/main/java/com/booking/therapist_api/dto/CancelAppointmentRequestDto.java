package com.booking.therapist_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelAppointmentRequestDto(
        @NotBlank(message = "reason is required")
        @Size(max = 1000, message = "reason must not exceed 1000 characters")
        String reason
) {
}
