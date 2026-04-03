package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.BookingRequestDto;
import com.booking.therapist_api.dto.BookingResponseDto;
import com.booking.therapist_api.dto.VideoJoinResponseDto;
import com.booking.therapist_api.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody BookingRequestDto request,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        UUID patientId = UUID.fromString(currentUser.getUsername());
        BookingResponseDto response = bookingService.processBooking(patientId, request.slotId());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{appointmentId}/join")
    public ResponseEntity<VideoJoinResponseDto> joinVideoSession(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(bookingService.joinVideoSession(appointmentId));
    }
}
