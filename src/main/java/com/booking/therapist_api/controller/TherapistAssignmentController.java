package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.ActiveAssignedTherapistResponse;
import com.booking.therapist_api.service.TherapistAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
public class TherapistAssignmentController {

    private final TherapistAssignmentService therapistAssignmentService;

    public TherapistAssignmentController(TherapistAssignmentService therapistAssignmentService) {
        this.therapistAssignmentService = therapistAssignmentService;
    }

    @GetMapping("/{profileId}/assigned-therapist")
    @PreAuthorize("#profileId.toString() == authentication.name or hasRole('ROLE_ADMIN')")
    public ResponseEntity<ActiveAssignedTherapistResponse> getActiveAssignedTherapist(
            @PathVariable UUID profileId
    ) {
        return ResponseEntity.ok(therapistAssignmentService.getActiveAssignedTherapist(profileId));
    }
}
