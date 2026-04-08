package com.booking.therapist_api.controller;

import com.booking.therapist_api.service.ScheduleGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestSchedulerController {

    private final ScheduleGenerationService scheduleGenerationService;

    public TestSchedulerController(ScheduleGenerationService scheduleGenerationService) {
        this.scheduleGenerationService = scheduleGenerationService;
    }

    @PostMapping("/trigger-generation")
    public ResponseEntity<Map<String, String>> triggerGeneration() {
        scheduleGenerationService.generateSlotsForNext30Days();
        return ResponseEntity.ok(Map.of("message", "Schedule slot generation triggered"));
    }

    @PostMapping("/trigger-cleanup")
    public ResponseEntity<Map<String, String>> triggerCleanup() {
        scheduleGenerationService.cleanupOldUnbookedSlots();
        return ResponseEntity.ok(Map.of("message", "Schedule slot cleanup triggered"));
    }
}
