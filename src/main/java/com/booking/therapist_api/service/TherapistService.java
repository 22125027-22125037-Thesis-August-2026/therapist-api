package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.ScheduleSlotResponseDto;
import com.booking.therapist_api.dto.TherapistDetailResponseDto;
import com.booking.therapist_api.entity.ScheduleSlot;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.entity.WeeklyTemplate;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ReviewRepository;
import com.booking.therapist_api.repository.ScheduleSlotRepository;
import com.booking.therapist_api.repository.TherapistRepository;
import com.booking.therapist_api.repository.WeeklyTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TherapistService {

    private final TherapistRepository therapistRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final WeeklyTemplateRepository weeklyTemplateRepository;
    private final ReviewRepository reviewRepository;

    public TherapistService(
            TherapistRepository therapistRepository,
            ScheduleSlotRepository scheduleSlotRepository,
            AppointmentRepository appointmentRepository,
            WeeklyTemplateRepository weeklyTemplateRepository,
            ReviewRepository reviewRepository
    ) {
        this.therapistRepository = therapistRepository;
        this.scheduleSlotRepository = scheduleSlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.weeklyTemplateRepository = weeklyTemplateRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public TherapistDetailResponseDto getTherapistDetail(UUID therapistId) {
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found for id: " + therapistId));

        long patientCount = appointmentRepository.countDistinctPatientsByTherapistId(therapistId);
        long reviewCount = reviewRepository.countByAppointment_Therapist_TherapistId(therapistId);

        List<TherapistDetailResponseDto.WorkingHour> workingHours = weeklyTemplateRepository
                .findByTherapist_TherapistIdAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(therapistId)
                .stream()
                .map(this::toWorkingHour)
                .toList();

        List<TherapistDetailResponseDto.ReviewItem> reviews = reviewRepository
                .findByAppointment_Therapist_TherapistIdOrderByCreatedAtDesc(therapistId)
                .stream()
                .map(review -> new TherapistDetailResponseDto.ReviewItem(
                        review.getReviewId().toString(),
                        "Anonymous Patient",
                        null,
                        review.getRating(),
                        review.getComment(),
                        review.getCreatedAt() == null ? null : review.getCreatedAt().toString()
                ))
                .toList();

        double averageRating = therapist.getRatingAvg() == null ? 0.0d : therapist.getRatingAvg().doubleValue();

        return new TherapistDetailResponseDto(
                therapist.getTherapistId().toString(),
                therapist.getFullName(),
                "",
                therapist.getSpecialization(),
                therapist.getCountry(),
                therapist.getAboutMe(),
                new TherapistDetailResponseDto.Stats(
                        Math.toIntExact(patientCount),
                        therapist.getYearsExperience() == null ? 0 : therapist.getYearsExperience(),
                        averageRating,
                        Math.toIntExact(reviewCount)
                ),
                workingHours,
                reviews
        );
    }

    @Transactional(readOnly = true)
    public Page<ScheduleSlotResponseDto> getAvailableSlots(UUID therapistId, Pageable pageable) {
        if (!therapistRepository.existsById(therapistId)) {
            throw new ResourceNotFoundException("Therapist not found for id: " + therapistId);
        }

        Instant nowUtc = Instant.now();
        return scheduleSlotRepository
                .findByTherapist_TherapistIdAndIsBookedFalseAndStartDatetimeAfterOrderByStartDatetimeAsc(
                        therapistId,
                        nowUtc,
                        pageable
                )
                .map(this::toResponseDto);
    }

    private ScheduleSlotResponseDto toResponseDto(ScheduleSlot slot) {
        return new ScheduleSlotResponseDto(slot.getId(), slot.getStartDatetime(), slot.getEndDatetime());
    }

    private TherapistDetailResponseDto.WorkingHour toWorkingHour(WeeklyTemplate weeklyTemplate) {
        String dayLabel = weeklyTemplate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return new TherapistDetailResponseDto.WorkingHour(
                dayLabel,
                weeklyTemplate.getStartTime().toString(),
                weeklyTemplate.getEndTime().toString()
        );
    }
}
