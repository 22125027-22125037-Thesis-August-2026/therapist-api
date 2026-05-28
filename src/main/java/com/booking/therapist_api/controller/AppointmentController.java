package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.AppointmentDetailResponseDto;
import com.booking.therapist_api.dto.AppointmentHistoryItemResponseDto;
import com.booking.therapist_api.dto.BookingRequestDto;
import com.booking.therapist_api.dto.BookingResponseDto;
import com.booking.therapist_api.dto.CancelAppointmentRequestDto;
import com.booking.therapist_api.dto.RejectAppointmentRequestDto;
import com.booking.therapist_api.dto.TherapistAppointmentItemDto;
import com.booking.therapist_api.dto.UpcomingAppointmentResponseDto;
import com.booking.therapist_api.dto.VideoJoinResponseDto;
import com.booking.therapist_api.enums.AppointmentStatus;
import com.booking.therapist_api.security.AuthUserDetails;
import com.booking.therapist_api.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
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
            @AuthenticationPrincipal String userId,
            Authentication authentication
    ) {
        UUID patientId = UUID.fromString(userId);

        String userEmail = "";
        String userName = "";
        if (authentication != null && authentication.getDetails() instanceof AuthUserDetails details) {
            userEmail = details.email() == null ? "" : details.email();
            userName = details.name() == null ? "" : details.name();
        }

        BookingResponseDto response = bookingService.processBooking(
                patientId, request.slotId(), userEmail, userName, request.reason(), request.mode());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/bookings/{appointmentId}/join")
    public ResponseEntity<VideoJoinResponseDto> joinVideoSession(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(bookingService.joinVideoSession(appointmentId));
    }

    @GetMapping("/bookings/{appointmentId}")
    @PreAuthorize("@therapistAuthorization.canViewAppointment(authentication, #appointmentId)")
    public ResponseEntity<AppointmentDetailResponseDto> getAppointmentDetail(
            @PathVariable UUID appointmentId
    ) {
        return ResponseEntity.ok(bookingService.getAppointmentDetail(appointmentId));
    }

    @PostMapping("/bookings/{appointmentId}/cancel")
    @PreAuthorize("@therapistAuthorization.canMutateAppointment(authentication, #appointmentId)")
    public ResponseEntity<AppointmentDetailResponseDto> cancelAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody CancelAppointmentRequestDto request
    ) {
        return ResponseEntity.ok(bookingService.cancelAppointment(appointmentId, request.reason()));
    }

    @PostMapping("/bookings/{appointmentId}/confirm")
    @PreAuthorize("@therapistAuthorization.canMutateAppointment(authentication, #appointmentId)")
    public ResponseEntity<AppointmentDetailResponseDto> confirmAppointment(
            @PathVariable UUID appointmentId
    ) {
        return ResponseEntity.ok(bookingService.confirmAppointment(appointmentId));
    }

    @PostMapping("/bookings/{appointmentId}/reject")
    @PreAuthorize("@therapistAuthorization.canMutateAppointment(authentication, #appointmentId)")
    public ResponseEntity<AppointmentDetailResponseDto> rejectAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody(required = false) RejectAppointmentRequestDto request
    ) {
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(bookingService.rejectAppointment(appointmentId, reason));
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

    @GetMapping("/profiles/{profileId}/appointments/unreviewed")
    @PreAuthorize("#profileId.toString() == authentication.name or hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<AppointmentHistoryItemResponseDto>> getCompletedUnreviewedAppointments(
            @PathVariable UUID profileId
    ) {
        return ResponseEntity.ok(bookingService.getCompletedUnreviewedAppointments(profileId));
    }

    @GetMapping("/therapists/{therapistId}/appointments")
    @PreAuthorize("@therapistAuthorization.isSelfTherapistOrAdmin(authentication, #therapistId)")
    public ResponseEntity<Page<TherapistAppointmentItemDto>> getTherapistAppointments(
            @PathVariable UUID therapistId,
            @RequestParam(required = false) List<AppointmentStatus> status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable
    ) {
        return ResponseEntity.ok(bookingService.getTherapistAppointments(therapistId, status, from, to, pageable));
    }
}
