package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.ClinicalNoteRequestDto;
import com.booking.therapist_api.dto.ClinicalNoteResponseDto;
import com.booking.therapist_api.service.ClinicalNoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
