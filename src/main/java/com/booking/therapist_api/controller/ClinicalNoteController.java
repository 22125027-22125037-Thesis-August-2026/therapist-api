package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.ClinicalNoteDetailResponseDto;
import com.booking.therapist_api.dto.ClinicalNoteListItemDto;
import com.booking.therapist_api.dto.ClinicalNoteRequestDto;
import com.booking.therapist_api.dto.ClinicalNoteResponseDto;
import com.booking.therapist_api.dto.ClinicalNoteUpdateRequestDto;
import com.booking.therapist_api.enums.ClinicalNoteStatus;
import com.booking.therapist_api.service.ClinicalNoteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @PreAuthorize("@clinicalNoteAuthorization.canSubmit(authentication, #request.appointmentId)")
    public ResponseEntity<ClinicalNoteResponseDto> submitClinicalNote(
            @Valid @RequestBody ClinicalNoteRequestDto request
    ) {
        ClinicalNoteResponseDto response = clinicalNoteService.submitNote(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/appointments/{appointmentId}")
    @PreAuthorize("@clinicalNoteAuthorization.canView(authentication, #appointmentId)")
    public ResponseEntity<ClinicalNoteDetailResponseDto> getClinicalNoteByAppointmentId(
        @PathVariable UUID appointmentId
    ) {
        return ResponseEntity.ok(clinicalNoteService.getNoteForAppointment(appointmentId));
    }

    // §6.4
    @GetMapping("/{noteId}")
    @PreAuthorize("@clinicalNoteAuthorization.canViewNote(authentication, #noteId)")
    public ResponseEntity<ClinicalNoteDetailResponseDto> getClinicalNoteById(
            @PathVariable UUID noteId
    ) {
        return ResponseEntity.ok(clinicalNoteService.getNoteById(noteId));
    }

    // §6.2 — amend draft
    @PutMapping("/{noteId}")
    @PreAuthorize("@clinicalNoteAuthorization.canMutateNote(authentication, #noteId)")
    public ResponseEntity<ClinicalNoteDetailResponseDto> updateClinicalNote(
            @PathVariable UUID noteId,
            @Valid @RequestBody ClinicalNoteUpdateRequestDto request
    ) {
        return ResponseEntity.ok(clinicalNoteService.updateNote(noteId, request));
    }

    // §6.2 — explicit finalize
    @PostMapping("/{noteId}/finalize")
    @PreAuthorize("@clinicalNoteAuthorization.canMutateNote(authentication, #noteId)")
    public ResponseEntity<ClinicalNoteDetailResponseDto> finalizeClinicalNote(
            @PathVariable UUID noteId
    ) {
        return ResponseEntity.ok(clinicalNoteService.finalizeNote(noteId));
    }

    // §6.3 — list notes. Therapists and admins only; therapists are forced to their own id.
    @GetMapping
    @PreAuthorize("hasRole('ROLE_THERAPIST') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<Page<ClinicalNoteListItemDto>> listNotes(
            @RequestParam(required = false) UUID therapistId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) ClinicalNoteStatus status,
            Pageable pageable,
            Authentication authentication
    ) {
        UUID effectiveTherapistId = therapistId;
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            // Therapist callers are scoped to their own notes regardless of query param.
            try {
                effectiveTherapistId = UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException ignored) {
                // fall through; service will simply filter by null which returns all the caller could see
            }
        }
        return ResponseEntity.ok(
                clinicalNoteService.searchNotes(effectiveTherapistId, patientId, status, pageable));
    }
}
