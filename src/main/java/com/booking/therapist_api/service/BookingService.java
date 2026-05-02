package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.BookingResponseDto;
import com.booking.therapist_api.dto.AppointmentHistoryItemResponseDto;
import com.booking.therapist_api.dto.UpcomingAppointmentResponseDto;
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
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final ScheduleSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final BookingEventPublisher eventPublisher;
    private final VideoConsultationProvider videoProvider;

    public BookingService(
            ScheduleSlotRepository slotRepository,
            AppointmentRepository appointmentRepository,
            BookingEventPublisher eventPublisher,
            VideoConsultationProvider videoProvider
    ) {
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
        this.videoProvider = videoProvider;
    }

    @Transactional
    public BookingResponseDto processBooking(UUID patientId, UUID slotId) {
        ScheduleSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found for id: " + slotId));

        int updatedRows = slotRepository.lockAndBookSlot(slotId);
        if (updatedRows == 0) {
            throw new SlotAlreadyBookedException("Slot is already booked for id: " + slotId);
        }

        String roomUrl = videoProvider.createVideoRoom();

        Appointment appointment = new Appointment();
        appointment.setProfileId(patientId);
        appointment.setTherapist(slot.getTherapist());
        appointment.setSlot(slot);
        appointment.setMode(AppointmentMode.VIDEO);
        appointment.setStartDatetime(slot.getStartDatetime());
        appointment.setMeetingLink(roomUrl);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        eventPublisher.publishAppointmentBooked(savedAppointment.getId());

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

        return new VideoJoinResponseDto(appointment.getMeetingLink());
    }

    @Transactional(readOnly = true)
    public UpcomingAppointmentResponseDto getClosestUpcomingAppointment(UUID profileId) {
        Appointment appointment = appointmentRepository
                .findFirstByProfileIdAndStartDatetimeAfterAndStatusOrderByStartDatetimeAsc(
                        profileId,
                        Instant.now(),
                        AppointmentStatus.UPCOMING
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No upcoming appointment found for profile id: " + profileId));

        return new UpcomingAppointmentResponseDto(
                appointment.getId(),
                appointment.getProfileId(),
                appointment.getTherapist().getTherapistId(),
                appointment.getSlot().getId(),
                appointment.getMode().name(),
                appointment.getStatus().name(),
                appointment.getStartDatetime()
        );
    }

    @Transactional(readOnly = true)
    public List<AppointmentHistoryItemResponseDto> getCompletedAndCancelledAppointments(UUID profileId) {
        List<AppointmentStatus> historyStatuses = List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED);
        return appointmentRepository
                .findByProfileIdAndStatusInOrderByStartDatetimeDesc(profileId, historyStatuses)
                .stream()
                .map(appointment -> new AppointmentHistoryItemResponseDto(
                        appointment.getId(),
                        appointment.getProfileId(),
                        appointment.getTherapist().getTherapistId(),
                        appointment.getTherapist().getFullName(),
                        appointment.getTherapist().getSpecialization(),
                        appointment.getTherapist().getCountry(),
                        appointment.getSlot().getId(),
                        appointment.getMode().name(),
                        appointment.getStatus().name(),
                        appointment.getStartDatetime()
                ))
                .toList();
    }
}
