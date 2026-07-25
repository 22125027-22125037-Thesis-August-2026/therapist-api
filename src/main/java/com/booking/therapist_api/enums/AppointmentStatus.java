package com.booking.therapist_api.enums;

public enum AppointmentStatus {
    REQUESTED,
    UPCOMING,
    IN_PROGRESS,
    /** Patient has submitted their review; the therapist has not yet finalized a clinical note. */
    PATIENT_COMPLETE,
    /** Therapist has finalized their clinical note; the patient has not yet submitted a review. */
    PROFESSIONAL_COMPLETE,
    /** Both the patient's review and the therapist's clinical note are done. */
    OVERALL_COMPLETE,
    CANCELLED;

    /** True for any of the three completion states, regardless of which side finished first. */
    public boolean isCompletionVariant() {
        return this == PATIENT_COMPLETE || this == PROFESSIONAL_COMPLETE || this == OVERALL_COMPLETE;
    }
}
