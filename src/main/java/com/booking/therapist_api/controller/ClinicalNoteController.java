package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.ClinicalNoteDetailResponseDto;
import com.booking.therapist_api.dto.ClinicalNoteRequestDto;
import com.booking.therapist_api.dto.ClinicalNoteResponseDto;
import com.booking.therapist_api.service.ClinicalNoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notes")
public class ClinicalNoteController {

    private final ClinicalNoteService clinicalNoteService;

    public ClinicalNoteController(ClinicalNoteService clinicalNoteService) {
        this.clinicalNoteService = clinicalNoteService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_THERAPIST')")
    public ResponseEntity<ClinicalNoteResponseDto> submitClinicalNote(
            @Valid @RequestBody ClinicalNoteRequestDto request
    ) {
        ClinicalNoteResponseDto response = clinicalNoteService.submitNote(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/appointments/{appointmentId}")
    @PreAuthorize("hasAnyRole('ROLE_PATIENT','ROLE_THERAPIST','ROLE_ADMIN')")
    public ResponseEntity<ClinicalNoteDetailResponseDto> getClinicalNoteByAppointmentId(
        @PathVariable UUID appointmentId,
        Authentication authentication
    ) {
    UUID requesterId = UUID.fromString(authentication.getName());
    boolean isTherapist = authentication.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_THERAPIST".equals(authority.getAuthority()));
    boolean isAdmin = authentication.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

    ClinicalNoteDetailResponseDto response = clinicalNoteService.getNoteForAppointment(
        appointmentId,
        requesterId,
        isTherapist,
        isAdmin
    );
    return ResponseEntity.ok(response);
    }
}
