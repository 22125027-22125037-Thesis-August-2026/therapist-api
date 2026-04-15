package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record AssignedTherapistSummary(
        UUID id,

        @JsonProperty("full_name")
        String fullName,

        String specialization,

        @JsonProperty("communication_style")
        String communicationStyle
) {
}
