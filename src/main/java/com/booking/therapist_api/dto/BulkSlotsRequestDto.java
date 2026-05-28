package com.booking.therapist_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkSlotsRequestDto(
        @NotEmpty(message = "slots must not be empty")
        @Valid
        List<CreateSlotRequestDto> slots
) {
}
