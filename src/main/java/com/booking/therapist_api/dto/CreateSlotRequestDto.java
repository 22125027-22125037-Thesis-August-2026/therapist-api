package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateSlotRequestDto(
        @NotNull(message = "startDatetime is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
        Instant startDatetime,

        @NotNull(message = "endDatetime is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
        Instant endDatetime
) {
}
