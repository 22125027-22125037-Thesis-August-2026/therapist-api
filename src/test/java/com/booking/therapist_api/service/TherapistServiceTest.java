package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.TherapistDetailResponseDto;
import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.entity.Review;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.entity.WeeklyTemplate;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ReviewRepository;
import com.booking.therapist_api.repository.ScheduleSlotRepository;
import com.booking.therapist_api.repository.TherapistRepository;
import com.booking.therapist_api.repository.WeeklyTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TherapistServiceTest {

    @Mock
    private TherapistRepository therapistRepository;

    @Mock
    private ScheduleSlotRepository scheduleSlotRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private WeeklyTemplateRepository weeklyTemplateRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private TherapistService therapistService;

    @Test
    void getTherapistDetail_returnsExpectedResponseShape() {
        UUID therapistId = UUID.randomUUID();

        Therapist therapist = new Therapist();
        therapist.setTherapistId(therapistId);
        therapist.setFullName("Dr. Sarah Johnson");
        therapist.setSpecialization("Anxiety & Panic Disorders");
        therapist.setCountry("United States");
        therapist.setAboutMe("Experienced therapist.");
        therapist.setYearsExperience(12);
        therapist.setRatingAvg(new BigDecimal("4.85"));

        WeeklyTemplate monday = new WeeklyTemplate();
        monday.setDayOfWeek(DayOfWeek.MONDAY);
        monday.setStartTime(LocalTime.of(8, 0));
        monday.setEndTime(LocalTime.of(16, 0));

        Appointment appointment = new Appointment();
        appointment.setProfileId(UUID.randomUUID());

        Review review = new Review();
        review.setReviewId(UUID.randomUUID());
        review.setAppointment(appointment);
        review.setRating(5);
        review.setComment("Very helpful session");
        review.setCreatedAt(Instant.parse("2026-04-16T10:15:30Z"));

        when(therapistRepository.findById(therapistId)).thenReturn(Optional.of(therapist));
        when(appointmentRepository.countDistinctPatientsByTherapistId(therapistId)).thenReturn(42L);
        when(reviewRepository.countByAppointment_Therapist_TherapistId(therapistId)).thenReturn(10L);
        when(weeklyTemplateRepository.findByTherapist_TherapistIdAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(therapistId))
                .thenReturn(List.of(monday));
        when(reviewRepository.findByAppointment_Therapist_TherapistIdOrderByCreatedAtDesc(therapistId))
                .thenReturn(List.of(review));

        TherapistDetailResponseDto response = therapistService.getTherapistDetail(therapistId);

        assertEquals(therapistId.toString(), response.id());
        assertEquals("Dr. Sarah Johnson", response.fullName());
        assertEquals("", response.avatarUrl());
        assertEquals("Anxiety & Panic Disorders", response.specialty());
        assertEquals("United States", response.location());
        assertEquals("Experienced therapist.", response.bio());
        assertEquals(42, response.stats().patientCount());
        assertEquals(12, response.stats().yearsOfExperience());
        assertEquals(4.85d, response.stats().averageRating());
        assertEquals(10, response.stats().reviewCount());
        assertEquals("Monday", response.workingHours().get(0).dayLabel());
        assertEquals("08:00", response.workingHours().get(0).startTime());
        assertEquals("16:00", response.workingHours().get(0).endTime());
        assertEquals("Anonymous Patient", response.reviews().get(0).reviewerName());
        assertEquals(5, response.reviews().get(0).rating());
    }

    @Test
    void getTherapistDetail_throwsWhenTherapistNotFound() {
        UUID therapistId = UUID.randomUUID();
        when(therapistRepository.findById(therapistId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> therapistService.getTherapistDetail(therapistId));
    }
}
