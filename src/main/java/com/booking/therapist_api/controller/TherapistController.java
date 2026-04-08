package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.ScheduleSlotResponseDto;
import com.booking.therapist_api.service.TherapistService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/therapists")
public class TherapistController {

    private final TherapistService therapistService;

    public TherapistController(TherapistService therapistService) {
        this.therapistService = therapistService;
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<Page<ScheduleSlotResponseDto>> getTherapistAvailableSlots(
            @PathVariable("id") UUID therapistId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(therapistService.getAvailableSlots(therapistId, pageable));
    }
}
