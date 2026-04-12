package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record MatchingPreferenceRequest(
        @NotBlank(message = "has_prior_counseling is required")
        @JsonProperty("has_prior_counseling")
        String hasPriorCounseling,

        @NotBlank(message = "gender is required")
        String gender,

        @NotBlank(message = "age is required")
        String age,

        @NotBlank(message = "sexual_orientation is required")
        @JsonProperty("sexual_orientation")
        String sexualOrientation,

        @NotNull(message = "is_lgbtq_priority is required")
        @JsonProperty("is_lgbtq_priority")
        Boolean isLgbtqPriority,

        @NotBlank(message = "self_harm_thought is required")
        @JsonProperty("self_harm_thought")
        String selfHarmThought,

        @NotEmpty(message = "reasons must not be empty")
        List<@NotBlank(message = "reason must not be blank") String> reasons,

        @NotEmpty(message = "mood_levels must not be empty")
        @JsonProperty("mood_levels")
        Map<@NotBlank(message = "mood level key must not be blank") String,
                @NotNull(message = "mood level value must not be null") Integer> moodLevels,

        @NotBlank(message = "communication_style is required")
        @JsonProperty("communication_style")
        String communicationStyle
) {
        @JsonIgnore
        @AssertTrue(message = "mood_levels must include exactly anxiety, lossInterest, and fatigue")
        public boolean hasRequiredMoodLevels() {
                if (moodLevels == null) {
                        return true;
                }
                return moodLevels.size() == 3
                                && moodLevels.containsKey("anxiety")
                                && moodLevels.containsKey("lossInterest")
                                && moodLevels.containsKey("fatigue");
        }
}
