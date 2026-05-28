package com.booking.therapist_api.dto;

import com.booking.therapist_api.enums.ClinicalNoteStatus;
import jakarta.validation.constraints.Size;

/**
 * Used for PUT /api/v1/notes/{noteId}. All fields are optional except those that the
 * caller wants to change. Passing status=FINALIZED here is equivalent to calling the
 * /finalize sub-resource.
 */
public record ClinicalNoteUpdateRequestDto(
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
