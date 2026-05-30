package com.booking.therapist_api.service;

import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.enums.AppointmentStatus;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ScheduleSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Auto-rejects pending booking requests once start_datetime is within
 * {@link BookingService#THERAPIST_DECISION_WINDOW} of now.
 *
 * Runs every minute; the read-time guards in BookingService.confirm/reject are
 * the immediate safety net that closes the up-to-60s race window.
 */
@Service
public class AppointmentAutoRejectService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentAutoRejectService.class);

    private static final String AUTO_REJECT_REASON =
            "Auto-rejected: therapist did not respond before the required decision window.";

    private final AppointmentRepository appointmentRepository;
    private final ScheduleSlotRepository slotRepository;

    public AppointmentAutoRejectService(
            AppointmentRepository appointmentRepository,
            ScheduleSlotRepository slotRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
    }

    @Transactional
    @Scheduled(fixedRateString = "${booking.auto-reject.interval-ms:60000}",
               initialDelayString = "${booking.auto-reject.initial-delay-ms:30000}")
    public int autoRejectExpiredRequests() {
        Instant cutoff = Instant.now().plus(BookingService.THERAPIST_DECISION_WINDOW);
        List<Appointment> expired = appointmentRepository.findExpiredRequests(
                AppointmentStatus.REQUESTED, cutoff);

        if (expired.isEmpty()) {
            return 0;
        }

        Instant now = Instant.now();
        for (Appointment appt : expired) {
            appt.setStatus(AppointmentStatus.CANCELLED);
            appt.setCancellationReason(AUTO_REJECT_REASON);
            appt.setCancelledAt(now);
            if (appt.getSlot() != null) {
                slotRepository.releaseSlot(appt.getSlot().getId());
            }
        }
        appointmentRepository.saveAll(expired);

        LOGGER.info(
                "Auto-reject sweep cancelled {} REQUESTED appointment(s) past the decision window.",
                expired.size());

        return expired.size();
    }
}
