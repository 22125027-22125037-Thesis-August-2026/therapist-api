package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.BookingRequestDto;
import com.booking.therapist_api.dto.BookingResponseDto;
import com.booking.therapist_api.dto.AppointmentHistoryItemResponseDto;
import com.booking.therapist_api.dto.UpcomingAppointmentResponseDto;
import com.booking.therapist_api.dto.VideoJoinResponseDto;
import com.booking.therapist_api.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AppointmentController {

    private final BookingService bookingService;

    public AppointmentController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody BookingRequestDto request,
            @AuthenticationPrincipal String userId
    ) {
        UUID patientId = UUID.fromString(userId);
        BookingResponseDto response = bookingService.processBooking(patientId, request.slotId());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/bookings/{appointmentId}/join")
    public ResponseEntity<VideoJoinResponseDto> joinVideoSession(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(bookingService.joinVideoSession(appointmentId));
    }

    @GetMapping("/profiles/{profileId}/appointments/upcoming")
    @PreAuthorize("#profileId.toString() == authentication.name or hasRole('ROLE_ADMIN')")
    public ResponseEntity<UpcomingAppointmentResponseDto> getClosestUpcomingAppointment(
            @PathVariable UUID profileId
    ) {
        return ResponseEntity.ok(bookingService.getClosestUpcomingAppointment(profileId));
    }

    @GetMapping("/profiles/{profileId}/appointments/history")
    @PreAuthorize("#profileId.toString() == authentication.name or hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<AppointmentHistoryItemResponseDto>> getCompletedAndCancelledAppointments(
            @PathVariable UUID profileId
    ) {
        return ResponseEntity.ok(bookingService.getCompletedAndCancelledAppointments(profileId));
    }
}
