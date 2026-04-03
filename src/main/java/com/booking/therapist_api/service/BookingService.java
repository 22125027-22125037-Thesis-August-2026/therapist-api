package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.BookingResponseDto;
import com.booking.therapist_api.dto.VideoJoinResponseDto;
import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.entity.ScheduleSlot;
import com.booking.therapist_api.enums.AppointmentMode;
import com.booking.therapist_api.enums.AppointmentStatus;
import com.booking.therapist_api.exception.MeetingNotOpenException;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.exception.SlotAlreadyBookedException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ScheduleSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class BookingService {

    private final ScheduleSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final RabbitMQPublisher eventPublisher;

    public BookingService(
            ScheduleSlotRepository slotRepository,
            AppointmentRepository appointmentRepository,
            RabbitMQPublisher eventPublisher
    ) {
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public BookingResponseDto processBooking(UUID patientId, UUID slotId) {
        ScheduleSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found for id: " + slotId));

        int updatedRows = slotRepository.lockAndBookSlot(slotId);
        if (updatedRows == 0) {
            throw new SlotAlreadyBookedException("Slot is already booked for id: " + slotId);
        }

        Appointment appointment = new Appointment();
        appointment.setProfileId(patientId);
        appointment.setTherapistId(slot.getTherapistId());
        appointment.setSlotId(slotId);
        appointment.setMode(AppointmentMode.VIDEO);
        appointment.setStartDatetime(slot.getStartDatetime());

        Appointment savedAppointment = appointmentRepository.save(appointment);
        eventPublisher.publishAppointmentBookedEvent(savedAppointment.getId());

        return new BookingResponseDto(
                savedAppointment.getId(),
                slotId,
                savedAppointment.getStatus().name(),
                "Booking created successfully"
        );
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
