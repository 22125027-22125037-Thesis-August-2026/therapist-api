package com.booking.therapist_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PatientTagsRequestDto(
        @NotNull(message = "tags is required")
        @Size(max = 20, message = "tags must not exceed 20 entries")
        List<@Size(max = 100, message = "tag must not exceed 100 characters") String> tags
) {
}
