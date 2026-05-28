package com.booking.therapist_api.dto;

import com.booking.therapist_api.enums.PatientRiskLevel;
import jakarta.validation.constraints.NotNull;

public record PatientRiskLevelRequestDto(
        @NotNull(message = "riskLevel is required") PatientRiskLevel riskLevel
) {
}
