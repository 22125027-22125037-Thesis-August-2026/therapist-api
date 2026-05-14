package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.ClinicalNoteDetailResponseDto;
import com.booking.therapist_api.dto.ClinicalNoteRequestDto;
import com.booking.therapist_api.dto.ClinicalNoteResponseDto;
import com.booking.therapist_api.service.ClinicalNoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
        ClinicalNoteDetailResponseDto response = clinicalNoteService.getNoteForAppointment(appointmentId);
        return ResponseEntity.ok(response);
    }
}
