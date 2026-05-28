package com.booking.therapist_api.dto;

public record TherapistDashboardSummaryDto(
        int activePatientCount,
        int completedThisMonth,
        double averageRating,
        int pendingBookingCount,
        int draftNoteCount,
        int moodAlertCount
) {
}
