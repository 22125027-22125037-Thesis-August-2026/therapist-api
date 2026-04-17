package com.booking.therapist_api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record TherapistDetailResponseDto(
        String id,
        String fullName,
        String avatarUrl,
        String specialty,
        String location,
        String bio,
        Stats stats,
        List<WorkingHour> workingHours,
        List<ReviewItem> reviews
) {
    public record Stats(
            int patientCount,
            int yearsOfExperience,
            double averageRating,
            int reviewCount
    ) {
    }

    public record WorkingHour(
            String dayLabel,
            String startTime,
            String endTime
    ) {
    }

        @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReviewItem(
            String id,
            String reviewerName,
            String reviewerAvatarUrl,
            int rating,
            String comment,
            String createdAt
    ) {
    }
}