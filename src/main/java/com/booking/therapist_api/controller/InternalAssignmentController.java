package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.InternalAssignmentPageDto;
import com.booking.therapist_api.service.InternalAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reconciliation snapshot of all therapist↔patient assignments. Mounted under
 * {@code /internal/}, which the nginx gateway 403s for external callers, so this
 * surface is reachable only from inside the cluster network.
 */
@RestController
public class InternalAssignmentController {

    private static final int MAX_PAGE_SIZE = 500;

    private final InternalAssignmentService internalAssignmentService;

    public InternalAssignmentController(InternalAssignmentService internalAssignmentService) {
        this.internalAssignmentService = internalAssignmentService;
    }

    @GetMapping("/internal/assignments")
    public ResponseEntity<InternalAssignmentPageDto> listAssignments(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return ResponseEntity.ok(internalAssignmentService.listAssignments(safePage, safeSize));
    }
}
