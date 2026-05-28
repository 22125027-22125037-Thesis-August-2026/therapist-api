package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.PatientMatchingPreferenceResponseDto;
import com.booking.therapist_api.dto.PatientRiskLevelRequestDto;
import com.booking.therapist_api.dto.PatientRiskLevelResponseDto;
import com.booking.therapist_api.dto.PatientTagsRequestDto;
import com.booking.therapist_api.dto.PatientTagsResponseDto;
import com.booking.therapist_api.dto.TherapistPatientItemDto;
import com.booking.therapist_api.service.TherapistPatientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class TherapistPatientController {

    private final TherapistPatientService patientService;

    public TherapistPatientController(TherapistPatientService patientService) {
        this.patientService = patientService;
    }

    // §4.1
    @GetMapping("/api/v1/therapists/{therapistId}/patients")
    @PreAuthorize("@therapistAuthorization.isSelfTherapistOrAdmin(authentication, #therapistId)")
    public ResponseEntity<List<TherapistPatientItemDto>> listPatients(@PathVariable UUID therapistId) {
        return ResponseEntity.ok(patientService.listPatients(therapistId));
    }

    // §4.3
    @GetMapping("/api/v1/patients/{profileId}/matching-preferences")
    @PreAuthorize("@therapistAuthorization.canAccessPatient(authentication, #profileId)")
    public ResponseEntity<PatientMatchingPreferenceResponseDto> getMatchingPreferences(
            @PathVariable UUID profileId
    ) {
        return ResponseEntity.ok(patientService.getMatchingPreferences(profileId));
    }

    // §4.4 — tags
    @GetMapping("/api/v1/patients/{profileId}/tags")
    @PreAuthorize("@therapistAuthorization.canAccessPatient(authentication, #profileId)")
    public ResponseEntity<PatientTagsResponseDto> getTags(@PathVariable UUID profileId) {
        return ResponseEntity.ok(patientService.getTags(profileId));
    }

    @PutMapping("/api/v1/patients/{profileId}/tags")
    @PreAuthorize("@therapistAuthorization.canAccessPatient(authentication, #profileId)")
    public ResponseEntity<PatientTagsResponseDto> putTags(
            @PathVariable UUID profileId,
            @Valid @RequestBody PatientTagsRequestDto request,
            @AuthenticationPrincipal String callerId
    ) {
        UUID updatedBy = parseUuidOrNull(callerId);
        return ResponseEntity.ok(patientService.upsertTags(profileId, updatedBy, request));
    }

    // §4.4 — risk level
    @GetMapping("/api/v1/patients/{profileId}/risk-level")
    @PreAuthorize("@therapistAuthorization.canAccessPatient(authentication, #profileId)")
    public ResponseEntity<PatientRiskLevelResponseDto> getRiskLevel(@PathVariable UUID profileId) {
        return ResponseEntity.ok(patientService.getRiskLevel(profileId));
    }

    @PutMapping("/api/v1/patients/{profileId}/risk-level")
    @PreAuthorize("@therapistAuthorization.canAccessPatient(authentication, #profileId)")
    public ResponseEntity<PatientRiskLevelResponseDto> putRiskLevel(
            @PathVariable UUID profileId,
            @Valid @RequestBody PatientRiskLevelRequestDto request,
            @AuthenticationPrincipal String callerId
    ) {
        UUID updatedBy = parseUuidOrNull(callerId);
        return ResponseEntity.ok(patientService.upsertRiskLevel(profileId, updatedBy, request));
    }

    private static UUID parseUuidOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
