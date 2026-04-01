package com.booking.therapist_api.controller;

import com.booking.therapist_api.dto.VideoJoinResponseDto;
import com.booking.therapist_api.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/appointments")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/{appointmentId}/join")
    public ResponseEntity<VideoJoinResponseDto> joinVideoSession(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(bookingService.joinVideoSession(appointmentId));
    }
}
