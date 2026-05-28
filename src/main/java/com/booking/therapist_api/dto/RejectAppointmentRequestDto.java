package com.booking.therapist_api.dto;

import jakarta.validation.constraints.Size;

public record RejectAppointmentRequestDto(
        @Size(max = 1000, message = "reason must not exceed 1000 characters")
        String reason
) {
}
