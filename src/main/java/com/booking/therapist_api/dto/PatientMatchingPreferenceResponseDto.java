package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatientMatchingPreferenceResponseDto(
        UUID profileId,

        @JsonProperty("has_prior_counseling")
        String hasPriorCounseling,

        @JsonProperty("sexual_orientation")
        String sexualOrientation,

        @JsonProperty("is_lgbtq_priority")
        Boolean isLgbtqPriority,

        List<String> reasons,

        @JsonProperty("communication_style")
        String communicationStyle,

        @JsonProperty("last_updated_at")
        Instant lastUpdatedAt
) {
}
