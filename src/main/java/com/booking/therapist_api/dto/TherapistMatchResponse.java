package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TherapistMatchResponse(
        UUID id,

        @JsonProperty("full_name")
        String fullName,

        String specialization,

        @JsonProperty("match_score")
        BigDecimal matchScore,

        @JsonProperty("matching_reasons")
        List<String> matchingReasons,

        @JsonProperty("communication_style")
        String communicationStyle
) {
}
