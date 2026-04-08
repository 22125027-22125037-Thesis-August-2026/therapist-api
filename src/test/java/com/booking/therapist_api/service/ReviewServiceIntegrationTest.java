package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.ReviewRequestDto;
import com.booking.therapist_api.dto.ReviewResponseDto;
import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.entity.ScheduleSlot;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.enums.AppointmentMode;
import com.booking.therapist_api.enums.AppointmentStatus;
import com.booking.therapist_api.exception.InvalidAppointmentStateException;
import com.booking.therapist_api.exception.ReviewAlreadyExistsException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ReviewRepository;
import com.booking.therapist_api.repository.TherapistRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewServiceIntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TherapistRepository therapistRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void submitReview_createsReview_andUpdatesTherapistAverage() {
        UUID patientId = UUID.randomUUID();
        Appointment firstAppointment = createAppointment(patientId, AppointmentStatus.COMPLETED, entityManager);

        ReviewResponseDto firstResponse = reviewService.submitReview(
                patientId,
                new ReviewRequestDto(firstAppointment.getId(), 4, "Good session")
        );

        assertEquals(4, firstResponse.rating());
        assertTrue(reviewRepository.existsByAppointment_Id(firstAppointment.getId()));

        Therapist therapistAfterFirstReview = therapistRepository
                .findById(firstAppointment.getTherapist().getTherapistId())
                .orElseThrow();
        assertEquals(new BigDecimal("4.00"), therapistAfterFirstReview.getRatingAvg());

        Appointment secondAppointment = createAppointment(patientId, AppointmentStatus.COMPLETED, therapistAfterFirstReview);
        reviewService.submitReview(patientId, new ReviewRequestDto(secondAppointment.getId(), 5, "Excellent"));

        Therapist therapistAfterSecondReview = therapistRepository
                .findById(therapistAfterFirstReview.getTherapistId())
                .orElseThrow();
        assertEquals(new BigDecimal("4.50"), therapistAfterSecondReview.getRatingAvg());
    }

    @Test
    void submitReview_rejectsNonCompletedAppointment() {
        UUID patientId = UUID.randomUUID();
        Appointment appointment = createAppointment(patientId, AppointmentStatus.UPCOMING, entityManager);

        assertThrows(
                InvalidAppointmentStateException.class,
                () -> reviewService.submitReview(patientId, new ReviewRequestDto(appointment.getId(), 5, "Too early"))
        );
    }

    @Test
    void submitReview_rejectsDuplicateReview() {
        UUID patientId = UUID.randomUUID();
        Appointment appointment = createAppointment(patientId, AppointmentStatus.COMPLETED, entityManager);

        reviewService.submitReview(patientId, new ReviewRequestDto(appointment.getId(), 5, "Nice"));

        assertThrows(
                ReviewAlreadyExistsException.class,
                () -> reviewService.submitReview(patientId, new ReviewRequestDto(appointment.getId(), 4, "Second review"))
        );
    }

    private Appointment createAppointment(UUID patientId, AppointmentStatus status, EntityManager entityManager) {
        Therapist therapist = new Therapist();
        therapist.setAccountId(UUID.randomUUID());
        therapist.setFullName("Review Test Therapist");
        therapist = entityManager.merge(therapist);

        return createAppointment(patientId, status, therapist);
    }

    private Appointment createAppointment(UUID patientId, AppointmentStatus status, Therapist therapist) {
        ScheduleSlot slot = new ScheduleSlot();
        slot.setTherapist(therapist);
        slot.setStartDatetime(Instant.now().plusSeconds(3600));
        slot.setEndDatetime(Instant.now().plusSeconds(5400));
        slot.setBooked(true);
        slot = entityManager.merge(slot);

        Appointment appointment = new Appointment();
        appointment.setProfileId(patientId);
        appointment.setTherapist(therapist);
        appointment.setSlot(slot);
        appointment.setMode(AppointmentMode.VIDEO);
        appointment.setStatus(status);
        appointment.setStartDatetime(slot.getStartDatetime());

        return appointmentRepository.save(appointment);
    }
}
