package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.AppointmentDetailResponseDto;
import com.booking.therapist_api.dto.AppointmentHistoryItemResponseDto;
import com.booking.therapist_api.dto.BookingResponseDto;
import com.booking.therapist_api.dto.TherapistAppointmentItemDto;
import com.booking.therapist_api.dto.UpcomingAppointmentResponseDto;
import com.booking.therapist_api.dto.VideoJoinResponseDto;
import com.booking.therapist_api.dto.VideoRoomDetailsDto;
import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.entity.ScheduleSlot;
import com.booking.therapist_api.enums.AppointmentMode;
import com.booking.therapist_api.enums.AppointmentStatus;
import com.booking.therapist_api.event.AppointmentBookedEvent;
import com.booking.therapist_api.exception.InvalidAppointmentStateException;
import com.booking.therapist_api.exception.MeetingNotOpenException;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.exception.SlotAlreadyBookedException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ScheduleSlotRepository;
import com.booking.therapist_api.repository.spec.AppointmentSpecifications;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    /** Therapists can no longer accept / reject a request once start_datetime is this close. */
    public static final Duration THERAPIST_DECISION_WINDOW = Duration.ofHours(2);

    /** Neither party can cancel an UPCOMING appointment once start_datetime is this close. */
    public static final Duration CANCEL_WINDOW = Duration.ofHours(1);

    private final ScheduleSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final VideoConsultationProvider videoProvider;

    public BookingService(
            ScheduleSlotRepository slotRepository,
            AppointmentRepository appointmentRepository,
            ApplicationEventPublisher eventPublisher,
            VideoConsultationProvider videoProvider
    ) {
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
        this.videoProvider = videoProvider;
    }

    @Transactional
    public BookingResponseDto processBooking(
            UUID patientId,
            UUID slotId,
            String userEmail,
            String userName,
            String reason,
            AppointmentMode mode
    ) {
        ScheduleSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found for id: " + slotId));

        // Bookings must give the therapist time to decide before auto-reject fires.
        Instant earliestAllowedStart = Instant.now().plus(THERAPIST_DECISION_WINDOW);
        if (slot.getStartDatetime().isBefore(earliestAllowedStart)) {
            throw new InvalidAppointmentStateException(
                    "Slot starts too soon. Bookings must be made at least "
                            + THERAPIST_DECISION_WINDOW.toHours()
                            + " hours before the appointment start time.");
        }

        int updatedRows = slotRepository.lockAndBookSlot(slotId);
        if (updatedRows == 0) {
            throw new SlotAlreadyBookedException("Slot is already booked for id: " + slotId);
        }

        VideoRoomDetailsDto roomDetails = videoProvider.getVideoRoomDetails(slot.getTherapist());

        Appointment appointment = new Appointment();
        appointment.setProfileId(patientId);
        appointment.setPatientName(userName == null ? "" : userName);
        appointment.setTherapist(slot.getTherapist());
        appointment.setSlot(slot);
        appointment.setMode(mode == null ? AppointmentMode.VIDEO : mode);
        // Patient bookings start in REQUESTED; the therapist must /confirm or /reject.
        appointment.setStatus(AppointmentStatus.REQUESTED);
        appointment.setStartDatetime(slot.getStartDatetime());
        appointment.setEndDatetime(slot.getEndDatetime());
        appointment.setReason(reason);
        appointment.setMeetingNumber(roomDetails.meetingNumber());
        appointment.setMeetingPassword(roomDetails.meetingPassword());

        Appointment savedAppointment = appointmentRepository.save(appointment);

        AppointmentBookedEvent event = new AppointmentBookedEvent(
                UUID.randomUUID(),
                Instant.now(),
                savedAppointment.getId(),
                savedAppointment.getProfileId(),
                userEmail == null ? "" : userEmail,
                userName == null ? "" : userName,
                savedAppointment.getTherapist().getFullName(),
                savedAppointment.getStartDatetime()
        );
        eventPublisher.publishEvent(event);

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

        String resolvedRoom = appointment.getMeetingNumber();

        VideoRoomDetailsDto roomDetails = videoProvider.getVideoRoomDetails(appointment.getTherapist());
        return new VideoJoinResponseDto(resolvedRoom, appointment.getMeetingPassword(), roomDetails.sdkJwt());
    }

    @Transactional(readOnly = true)
    public UpcomingAppointmentResponseDto getClosestUpcomingAppointment(UUID profileId) {
        Instant now = Instant.now();
        Instant recentCutoff = now.minus(30, ChronoUnit.MINUTES);

        Appointment appointment = appointmentRepository
            .findClosestUpcomingOrRecentInProgress(
                profileId,
                List.of(AppointmentStatus.REQUESTED, AppointmentStatus.UPCOMING, AppointmentStatus.IN_PROGRESS),
                recentCutoff
            )
                .orElseThrow(() -> new ResourceNotFoundException(
                "No upcoming or ongoing appointment found for profile id: " + profileId));

        return new UpcomingAppointmentResponseDto(
                appointment.getId(),
                appointment.getProfileId(),
                appointment.getTherapist().getTherapistId(),
                appointment.getSlot().getId(),
                appointment.getMode().name(),
                appointment.getStatus().name(),
                appointment.getStartDatetime(),
                appointment.getEndDatetime(),
                appointment.getReason()
        );
    }

    @Transactional(readOnly = true)
    public List<AppointmentHistoryItemResponseDto> getCompletedAndCancelledAppointments(UUID profileId) {
        List<AppointmentStatus> historyStatuses = List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED);
        return appointmentRepository
                .findByProfileIdAndStatusInOrderByStartDatetimeDesc(profileId, historyStatuses)
                .stream()
                .map(this::toHistoryItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentHistoryItemResponseDto> getCompletedUnreviewedAppointments(UUID profileId) {
        return appointmentRepository
            .findByProfileIdAndStatusAndReviewIsNullOrderByStartDatetimeDesc(
                profileId,
                AppointmentStatus.COMPLETED
            )
            .stream()
            .map(this::toHistoryItem)
            .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentDetailResponseDto getAppointmentDetail(UUID appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found for id: " + appointmentId));
        return toAppointmentDetail(appt);
    }

    @Transactional(readOnly = true)
    public Page<TherapistAppointmentItemDto> getTherapistAppointments(
            UUID therapistId,
            Collection<AppointmentStatus> statuses,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        Specification<Appointment> spec =
                AppointmentSpecifications.ofTherapist(therapistId, statuses, from, to);
        return appointmentRepository.findAll(spec, pageable).map(this::toTherapistItem);
    }

    @Transactional
    public AppointmentDetailResponseDto cancelAppointment(UUID appointmentId, String reason) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found for id: " + appointmentId));

        if (appt.getStatus() == AppointmentStatus.CANCELLED
                || appt.getStatus() == AppointmentStatus.COMPLETED) {
            throw new InvalidAppointmentStateException(
                    "Appointment cannot be cancelled. Current status: " + appt.getStatus().name());
        }

        // UPCOMING appointments can no longer be cancelled inside the 1h window.
        // REQUESTED has its own deadline (the auto-reject sweep) so the cancel-window
        // rule deliberately does not apply there.
        if (appt.getStatus() == AppointmentStatus.UPCOMING) {
            Instant cutoff = appt.getStartDatetime().minus(CANCEL_WINDOW);
            if (Instant.now().isAfter(cutoff)) {
                throw new InvalidAppointmentStateException(
                        "Confirmed appointments cannot be cancelled within "
                                + CANCEL_WINDOW.toHours() + " hour(s) of the start time.");
            }
        }

        appt.setStatus(AppointmentStatus.CANCELLED);
        appt.setCancellationReason(reason);
        appt.setCancelledAt(Instant.now());
        appointmentRepository.save(appt);

        // Release the slot so it can be re-booked.
        if (appt.getSlot() != null) {
            slotRepository.releaseSlot(appt.getSlot().getId());
        }

        return toAppointmentDetail(appt);
    }

    @Transactional
    public AppointmentDetailResponseDto confirmAppointment(UUID appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found for id: " + appointmentId));

        if (appt.getStatus() != AppointmentStatus.REQUESTED) {
            throw new InvalidAppointmentStateException(
                    "Only REQUESTED appointments can be confirmed. Current status: "
                            + appt.getStatus().name());
        }

        // Race-condition safety net: the auto-reject sweep runs every 60s, but a
        // therapist may click Accept inside that window. Reject server-side too.
        Instant deadline = appt.getStartDatetime().minus(THERAPIST_DECISION_WINDOW);
        if (Instant.now().isAfter(deadline)) {
            throw new InvalidAppointmentStateException(
                    "Decision window has passed. The therapist can no longer accept this request.");
        }

        appt.setStatus(AppointmentStatus.UPCOMING);
        appointmentRepository.save(appt);
        return toAppointmentDetail(appt);
    }

    @Transactional
    public AppointmentDetailResponseDto rejectAppointment(UUID appointmentId, String reason) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found for id: " + appointmentId));

        if (appt.getStatus() != AppointmentStatus.REQUESTED) {
            throw new InvalidAppointmentStateException(
                    "Only REQUESTED appointments can be rejected. Current status: "
                            + appt.getStatus().name());
        }

        Instant deadline = appt.getStartDatetime().minus(THERAPIST_DECISION_WINDOW);
        if (Instant.now().isAfter(deadline)) {
            throw new InvalidAppointmentStateException(
                    "Decision window has passed. This request has already been auto-rejected.");
        }

        appt.setStatus(AppointmentStatus.CANCELLED);
        appt.setCancellationReason(reason);
        appt.setCancelledAt(Instant.now());
        appointmentRepository.save(appt);

        if (appt.getSlot() != null) {
            slotRepository.releaseSlot(appt.getSlot().getId());
        }
        return toAppointmentDetail(appt);
    }

    private AppointmentHistoryItemResponseDto toHistoryItem(Appointment appointment) {
        return new AppointmentHistoryItemResponseDto(
                appointment.getId(),
                appointment.getProfileId(),
                appointment.getTherapist().getTherapistId(),
                appointment.getTherapist().getFullName(),
                appointment.getTherapist().getSpecialization(),
                appointment.getTherapist().getCountry(),
                appointment.getSlot().getId(),
                appointment.getMode().name(),
                appointment.getStatus().name(),
                appointment.getStartDatetime(),
                appointment.getEndDatetime(),
                appointment.getReason()
        );
    }

    private TherapistAppointmentItemDto toTherapistItem(Appointment appointment) {
        return new TherapistAppointmentItemDto(
                appointment.getId(),
                appointment.getProfileId(),
                appointment.getPatientName(),
                appointment.getTherapist().getTherapistId(),
                appointment.getSlot().getId(),
                appointment.getMode().name(),
                appointment.getStatus().name(),
                appointment.getStartDatetime(),
                appointment.getEndDatetime(),
                appointment.getReason()
        );
    }

    private AppointmentDetailResponseDto toAppointmentDetail(Appointment appointment) {
        return new AppointmentDetailResponseDto(
                appointment.getId(),
                appointment.getProfileId(),
                appointment.getPatientName(),
                appointment.getTherapist().getTherapistId(),
                appointment.getTherapist().getFullName(),
                appointment.getTherapist().getSpecialization(),
                appointment.getSlot().getId(),
                appointment.getMode().name(),
                appointment.getStatus().name(),
                appointment.getStartDatetime(),
                appointment.getEndDatetime(),
                appointment.getReason(),
                appointment.getCancellationReason(),
                appointment.getCancelledAt(),
                appointment.getCreatedAt()
        );
    }
}
