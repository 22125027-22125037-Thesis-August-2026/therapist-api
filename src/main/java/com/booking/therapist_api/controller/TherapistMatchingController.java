package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.MatchingPreferenceRequest;
import com.booking.therapist_api.dto.TherapistMatchResponse;
import com.booking.therapist_api.service.TherapistMatchingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching")
public class TherapistMatchingController {

    private final TherapistMatchingService therapistMatchingService;

    public TherapistMatchingController(TherapistMatchingService therapistMatchingService) {
        this.therapistMatchingService = therapistMatchingService;
    }

    @PostMapping("/preferences")
    public ResponseEntity<Void> savePreferences(
            @Valid @RequestBody MatchingPreferenceRequest request,
            @AuthenticationPrincipal String userId
    ) {
        UUID profileId = UUID.fromString(userId);
        therapistMatchingService.savePreferences(profileId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/therapists")
    public ResponseEntity<List<TherapistMatchResponse>> findMatches(
            @AuthenticationPrincipal String userId
    ) {
        UUID profileId = UUID.fromString(userId);
        return ResponseEntity.ok(therapistMatchingService.findMatches(profileId));
    }

    @PostMapping("/assign/{therapistId}")
    public ResponseEntity<Void> assignTherapist(
            @PathVariable UUID therapistId,
            @AuthenticationPrincipal String userId
    ) {
        UUID profileId = UUID.fromString(userId);
        therapistMatchingService.assignTherapist(profileId, therapistId);
        return ResponseEntity.noContent().build();
    }
}
