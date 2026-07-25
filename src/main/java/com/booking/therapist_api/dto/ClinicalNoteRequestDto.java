package com.booking.therapist_api.dto;

import com.booking.therapist_api.enums.ClinicalNoteStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Used for both POST /api/v1/notes (create) and amendments.
 *
 * status is optional. If omitted the note is treated as FINALIZED (back-compat with
 * the original single-step submit flow). DRAFT bypasses the IN_PROGRESS state guard
 * and does NOT flip the appointment to PROFESSIONAL_COMPLETE / OVERALL_COMPLETE.
 *
 * diagnosis / recommendations are required for FINALIZED notes and optional for DRAFT.
 */
public record ClinicalNoteRequestDto(
        @NotNull(message = "appointmentId is required")
        UUID appointmentId,

        @Size(max = 4000) String diagnosis,
        @Size(max = 4000) String recommendations,
        @Size(max = 4000) String subjective,
        @Size(max = 4000) String objective,
        @Size(max = 4000) String assessment,
        @Size(max = 4000) String plan,
        @Size(max = 500)  String summary,

        Boolean riskSuicidalIdeation,
        Boolean riskSelfHarm,
        Boolean riskSubstanceUse,
        Boolean riskAbuse,

        ClinicalNoteStatus status
) {
}
