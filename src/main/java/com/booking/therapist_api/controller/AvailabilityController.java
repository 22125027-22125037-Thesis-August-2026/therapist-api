package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.BulkSlotsRequestDto;
import com.booking.therapist_api.dto.CreateSlotRequestDto;
import com.booking.therapist_api.dto.TherapistSlotResponseDto;
import com.booking.therapist_api.dto.WeeklyTemplateRequestDto;
import com.booking.therapist_api.dto.WeeklyTemplateResponseDto;
import com.booking.therapist_api.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/therapists/{therapistId}")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    // -----------------------------------------------------------------------------------
    // Slots — §3.1 / §3.2
    // -----------------------------------------------------------------------------------

    @GetMapping("/slots/manage")
    @PreAuthorize("@therapistAuthorization.isSelfTherapistOrAdmin(authentication, #therapistId)")
    public ResponseEntity<Page<TherapistSlotResponseDto>> listSlots(
            @PathVariable UUID therapistId,
            @RequestParam(defaultValue = "false") boolean includeBooked,
            Pageable pageable
    ) {
        return ResponseEntity.ok(availabilityService.getSlots(therapistId, includeBooked, pageable));
    }

    @PostMapping("/slots")
    @PreAuthorize("@therapistAuthorization.isSelfTherapistOrAdmin(authentication, #therapistId)")
    public ResponseEntity<TherapistSlotResponseDto> createSlot(
            @PathVariable UUID therapistId,
            @Valid @RequestBody CreateSlotRequestDto request
    ) {
        return ResponseEntity.status(201).body(availabilityService.createSlot(therapistId, request));
    }

    @PostMapping("/slots:bulk")
    @PreAuthorize("@therapistAuthorization.isSelfTherapistOrAdmin(authentication, #therapistId)")
    public ResponseEntity<List<TherapistSlotResponseDto>> createSlotsBulk(
            @PathVariable UUID therapistId,
            @Valid @RequestBody BulkSlotsRequestDto request
    ) {
        return ResponseEntity.status(201).body(availabilityService.createSlotsBulk(therapistId, request));
    }

    @PutMapping("/slots/{slotId}")
    @PreAuthorize("@therapistAuthorization.isSelfTherapistOrAdmin(authentication, #therapistId)")
    public ResponseEntity<TherapistSlotResponseDto> updateSlot(
            @PathVariable UUID therapistId,
            @PathVariable UUID slotId,
            @Valid @RequestBody CreateSlotRequestDto request
    ) {
        return ResponseEntity.ok(availabilityService.updateSlot(therapistId, slotId, request));
    }

    @DeleteMapping("/slots/{slotId}")
    @PreAuthorize("@therapistAuthorization.isSelfTherapistOrAdmin(authentication, #therapistId)")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable UUID therapistId,
            @PathVariable UUID slotId
    ) {
        availabilityService.deleteSlot(therapistId, slotId);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------------------
    // Weekly templates — §3.3
    // -----------------------------------------------------------------------------------

    @GetMapping("/availability-templates")
    @PreAuthorize("@therapistAuthorization.isSelfTherapistOrAdmin(authentication, #therapistId)")
    public ResponseEntity<List<WeeklyTemplateResponseDto>> listTemplates(@PathVariable UUID therapistId) {
        return ResponseEntity.ok(availabilityService.listTemplates(therapistId));
    }

    @PostMapping("/availability-templates")
    @PreAuthorize("@therapistAuthorization.isSelfTherapistOrAdmin(authentication, #therapistId)")
    public ResponseEntity<WeeklyTemplateResponseDto> createTemplate(
            @PathVariable UUID therapistId,
            @Valid @RequestBody WeeklyTemplateRequestDto request
    ) {
        return ResponseEntity.status(201).body(availabilityService.createTemplate(therapistId, request));
    }

    @PutMapping("/availability-templates/{templateId}")
    @PreAuthorize("@therapistAuthorization.isSelfTherapistOrAdmin(authentication, #therapistId)")
    public ResponseEntity<WeeklyTemplateResponseDto> updateTemplate(
            @PathVariable UUID therapistId,
            @PathVariable UUID templateId,
            @Valid @RequestBody WeeklyTemplateRequestDto request
    ) {
        return ResponseEntity.ok(availabilityService.updateTemplate(therapistId, templateId, request));
    }

    @DeleteMapping("/availability-templates/{templateId}")
    @PreAuthorize("@therapistAuthorization.isSelfTherapistOrAdmin(authentication, #therapistId)")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID therapistId,
            @PathVariable UUID templateId
    ) {
        availabilityService.deleteTemplate(therapistId, templateId);
        return ResponseEntity.noContent().build();
    }
}
