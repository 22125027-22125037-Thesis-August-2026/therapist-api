package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TherapistReviewResponseDto(
        String id,
        String reviewerName,
        String reviewerAvatarUrl,
        int rating,
        String comment,
        String createdAt
) {
}
