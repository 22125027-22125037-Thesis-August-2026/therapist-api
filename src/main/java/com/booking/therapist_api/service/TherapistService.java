package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.ScheduleSlotResponseDto;
import com.booking.therapist_api.entity.ScheduleSlot;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.ScheduleSlotRepository;
import com.booking.therapist_api.repository.TherapistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TherapistService {

    private final TherapistRepository therapistRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;

    public TherapistService(TherapistRepository therapistRepository, ScheduleSlotRepository scheduleSlotRepository) {
        this.therapistRepository = therapistRepository;
        this.scheduleSlotRepository = scheduleSlotRepository;
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
}
