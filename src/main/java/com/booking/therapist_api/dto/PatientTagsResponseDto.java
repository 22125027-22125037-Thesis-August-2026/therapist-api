package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatientTagsResponseDto(
        UUID profileId,
        List<String> tags,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant updatedAt,
        UUID updatedBy
) {
}
