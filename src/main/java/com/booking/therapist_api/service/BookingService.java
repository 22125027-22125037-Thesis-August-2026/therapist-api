package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.VideoJoinResponseDto;
import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.enums.AppointmentStatus;
import com.booking.therapist_api.exception.MeetingNotOpenException;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class BookingService {

    private final AppointmentRepository appointmentRepository;

    public BookingService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    public VideoJoinResponseDto joinVideoSession(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found for id: " + appointmentId));

        Instant allowedStartTime = appointment.getStartDatetime().minus(10, ChronoUnit.MINUTES);
        if (Instant.now().isBefore(allowedStartTime)) {
            throw new MeetingNotOpenException("The video room opens 10 minutes before the scheduled time.");
        }

        if (appointment.getStatus() == AppointmentStatus.UPCOMING) {
            appointment.setStatus(AppointmentStatus.IN_PROGRESS);
            appointmentRepository.save(appointment);
        }

        return new VideoJoinResponseDto(appointment.getMeetingLink(), "mock-jwt-token-for-now");
    }
}
