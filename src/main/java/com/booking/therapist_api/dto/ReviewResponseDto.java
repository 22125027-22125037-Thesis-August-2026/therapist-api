package com.booking.therapist_api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReviewResponseDto(
        UUID reviewId,
        UUID appointmentId,
        UUID therapistId,
        Integer rating,
        BigDecimal therapistRatingAvg,
        Instant createdAt,
        String message
) {
}
