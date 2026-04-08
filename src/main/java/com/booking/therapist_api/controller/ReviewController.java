package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.ReviewRequestDto;
import com.booking.therapist_api.dto.ReviewResponseDto;
import com.booking.therapist_api.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    public ResponseEntity<ReviewResponseDto> submitReview(
            @Valid @RequestBody ReviewRequestDto request,
            @AuthenticationPrincipal String userId
    ) {
        UUID patientId = UUID.fromString(userId);
        ReviewResponseDto response = reviewService.submitReview(patientId, request);
        return ResponseEntity.status(201).body(response);
    }
}
