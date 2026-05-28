package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.TherapistDashboardSummaryDto;
import com.booking.therapist_api.service.TherapistDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class TherapistDashboardController {

    private final TherapistDashboardService dashboardService;

    public TherapistDashboardController(TherapistDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/v1/therapists/{therapistId}/dashboard/summary")
    @PreAuthorize("@therapistAuthorization.isSelfTherapistOrAdmin(authentication, #therapistId)")
    public ResponseEntity<TherapistDashboardSummaryDto> getSummary(@PathVariable UUID therapistId) {
        return ResponseEntity.ok(dashboardService.getSummary(therapistId));
    }
}
