package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.TherapistDashboardSummaryDto;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.enums.AppointmentStatus;
import com.booking.therapist_api.enums.AssignmentStatus;
import com.booking.therapist_api.enums.ClinicalNoteStatus;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ClinicalNoteRepository;
import com.booking.therapist_api.repository.TherapistAssignmentRepository;
import com.booking.therapist_api.repository.TherapistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class TherapistDashboardService {

    private final TherapistRepository therapistRepository;
    private final TherapistAssignmentRepository assignmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;

    public TherapistDashboardService(
            TherapistRepository therapistRepository,
            TherapistAssignmentRepository assignmentRepository,
            AppointmentRepository appointmentRepository,
            ClinicalNoteRepository clinicalNoteRepository
    ) {
        this.therapistRepository = therapistRepository;
        this.assignmentRepository = assignmentRepository;
        this.appointmentRepository = appointmentRepository;
        this.clinicalNoteRepository = clinicalNoteRepository;
    }

    @Transactional(readOnly = true)
    public TherapistDashboardSummaryDto getSummary(UUID therapistId) {
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Therapist not found for id: " + therapistId));

        int activePatientCount = assignmentRepository
                .findAllByTherapist_TherapistIdAndStatus(therapistId, AssignmentStatus.ACTIVE)
                .size();

        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        Instant monthStart = nowUtc.toLocalDate()
                .withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        Instant nextMonthStart = nowUtc.toLocalDate()
                .withDayOfMonth(1)
                .plusMonths(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        int completedThisMonth = Math.toIntExact(
                appointmentRepository.countCompletedByTherapistInRange(
                        therapistId, AppointmentStatus.COMPLETED, monthStart, nextMonthStart));

        int pendingBookingCount = Math.toIntExact(
                appointmentRepository.countByTherapist_TherapistIdAndStatus(
                        therapistId, AppointmentStatus.REQUESTED));

        int draftNoteCount = Math.toIntExact(
                clinicalNoteRepository.countByAppointment_Therapist_TherapistIdAndStatus(
                        therapistId, ClinicalNoteStatus.DRAFT));

        double averageRating = therapist.getRatingAvg() == null
                ? 0.0d
                : therapist.getRatingAvg().doubleValue();

        // Mood alerts are owned by the Tracking Service. The therapist-api surfaces
        // a stable 0 so the client contract is consistent until the tracking-side
        // /alerts endpoint is wired in.
        int moodAlertCount = 0;

        return new TherapistDashboardSummaryDto(
                activePatientCount,
                completedThisMonth,
                averageRating,
                pendingBookingCount,
                draftNoteCount,
                moodAlertCount
        );
    }
}
