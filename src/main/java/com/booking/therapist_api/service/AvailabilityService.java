package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.BulkSlotsRequestDto;
import com.booking.therapist_api.dto.CreateSlotRequestDto;
import com.booking.therapist_api.dto.TherapistSlotResponseDto;
import com.booking.therapist_api.dto.WeeklyTemplateRequestDto;
import com.booking.therapist_api.dto.WeeklyTemplateResponseDto;
import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.entity.ScheduleSlot;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.entity.WeeklyTemplate;
import com.booking.therapist_api.exception.InvalidAppointmentStateException;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.exception.SlotConflictException;
import com.booking.therapist_api.repository.ScheduleSlotRepository;
import com.booking.therapist_api.repository.TherapistRepository;
import com.booking.therapist_api.repository.WeeklyTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AvailabilityService {

    private final ScheduleSlotRepository slotRepository;
    private final WeeklyTemplateRepository weeklyTemplateRepository;
    private final TherapistRepository therapistRepository;

    public AvailabilityService(
            ScheduleSlotRepository slotRepository,
            WeeklyTemplateRepository weeklyTemplateRepository,
            TherapistRepository therapistRepository
    ) {
        this.slotRepository = slotRepository;
        this.weeklyTemplateRepository = weeklyTemplateRepository;
        this.therapistRepository = therapistRepository;
    }

    // -----------------------------------------------------------------------------------
    // Slots
    // -----------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<TherapistSlotResponseDto> getSlots(
            UUID therapistId,
            boolean includeBooked,
            Pageable pageable
    ) {
        if (!therapistRepository.existsById(therapistId)) {
            throw new ResourceNotFoundException("Therapist not found for id: " + therapistId);
        }

        Instant now = Instant.now();
        Page<ScheduleSlot> page = includeBooked
                ? slotRepository.findByTherapist_TherapistIdAndStartDatetimeAfterOrderByStartDatetimeAsc(therapistId, now, pageable)
                : slotRepository.findByTherapist_TherapistIdAndIsBookedFalseAndStartDatetimeAfterOrderByStartDatetimeAsc(therapistId, now, pageable);

        return page.map(this::toSlotResponse);
    }

    @Transactional
    public TherapistSlotResponseDto createSlot(UUID therapistId, CreateSlotRequestDto request) {
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found for id: " + therapistId));

        validateSlotWindow(request.startDatetime(), request.endDatetime());

        if (slotRepository.overlapsExisting(therapistId, request.startDatetime(), request.endDatetime())) {
            throw new SlotConflictException(
                    "Slot overlaps an existing slot for this therapist.");
        }

        ScheduleSlot slot = new ScheduleSlot();
        slot.setTherapist(therapist);
        slot.setStartDatetime(request.startDatetime());
        slot.setEndDatetime(request.endDatetime());
        slot.setBooked(false);

        ScheduleSlot saved = slotRepository.save(slot);
        return toSlotResponse(saved);
    }

    @Transactional
    public List<TherapistSlotResponseDto> createSlotsBulk(UUID therapistId, BulkSlotsRequestDto request) {
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found for id: " + therapistId));

        List<ScheduleSlot> toSave = new ArrayList<>();
        for (CreateSlotRequestDto item : request.slots()) {
            validateSlotWindow(item.startDatetime(), item.endDatetime());
            if (slotRepository.overlapsExisting(therapistId, item.startDatetime(), item.endDatetime())) {
                throw new SlotConflictException(
                        "Slot overlaps an existing slot for this therapist: "
                                + item.startDatetime() + " — " + item.endDatetime());
            }
            ScheduleSlot slot = new ScheduleSlot();
            slot.setTherapist(therapist);
            slot.setStartDatetime(item.startDatetime());
            slot.setEndDatetime(item.endDatetime());
            slot.setBooked(false);
            toSave.add(slot);
        }

        return slotRepository.saveAll(toSave).stream()
                .map(this::toSlotResponse)
                .toList();
    }

    @Transactional
    public TherapistSlotResponseDto updateSlot(UUID therapistId, UUID slotId, CreateSlotRequestDto request) {
        ScheduleSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found for id: " + slotId));

        if (!slot.getTherapist().getTherapistId().equals(therapistId)) {
            throw new ResourceNotFoundException(
                    "Slot " + slotId + " does not belong to therapist " + therapistId);
        }

        if (slot.isBooked()) {
            throw new InvalidAppointmentStateException(
                    "Booked slots cannot be edited. Cancel the appointment first.");
        }

        validateSlotWindow(request.startDatetime(), request.endDatetime());

        if (slotRepository.overlapsExistingExcluding(therapistId, slotId,
                request.startDatetime(), request.endDatetime())) {
            throw new SlotConflictException(
                    "Updated slot overlaps an existing slot for this therapist.");
        }

        slot.setStartDatetime(request.startDatetime());
        slot.setEndDatetime(request.endDatetime());
        ScheduleSlot saved = slotRepository.save(slot);
        return toSlotResponse(saved);
    }

    @Transactional
    public void deleteSlot(UUID therapistId, UUID slotId) {
        ScheduleSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found for id: " + slotId));

        if (!slot.getTherapist().getTherapistId().equals(therapistId)) {
            throw new ResourceNotFoundException(
                    "Slot " + slotId + " does not belong to therapist " + therapistId);
        }

        if (slot.isBooked()) {
            throw new InvalidAppointmentStateException(
                    "Booked slots cannot be deleted. Cancel the appointment first.");
        }

        slotRepository.delete(slot);
    }

    private void validateSlotWindow(Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw new SlotConflictException("endDatetime must be after startDatetime.");
        }
        if (start.isBefore(Instant.now())) {
            throw new SlotConflictException("Slot startDatetime must be in the future.");
        }
    }

    private TherapistSlotResponseDto toSlotResponse(ScheduleSlot slot) {
        Appointment appt = slot.getAppointment();
        if (appt == null) {
            return new TherapistSlotResponseDto(
                    slot.getId(),
                    slot.getStartDatetime(),
                    slot.getEndDatetime(),
                    slot.isBooked(),
                    null,
                    null,
                    null
            );
        }
        return new TherapistSlotResponseDto(
                slot.getId(),
                slot.getStartDatetime(),
                slot.getEndDatetime(),
                slot.isBooked(),
                appt.getProfileId(),
                appt.getPatientName(),
                appt.getId()
        );
    }

    // -----------------------------------------------------------------------------------
    // Weekly templates
    // -----------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<WeeklyTemplateResponseDto> listTemplates(UUID therapistId) {
        if (!therapistRepository.existsById(therapistId)) {
            throw new ResourceNotFoundException("Therapist not found for id: " + therapistId);
        }
        return weeklyTemplateRepository
                .findByTherapist_TherapistIdOrderByDayOfWeekAscStartTimeAsc(therapistId)
                .stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    @Transactional
    public WeeklyTemplateResponseDto createTemplate(UUID therapistId, WeeklyTemplateRequestDto request) {
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found for id: " + therapistId));

        validateTemplateWindow(request);

        WeeklyTemplate template = new WeeklyTemplate();
        template.setTherapist(therapist);
        template.setDayOfWeek(request.dayOfWeek());
        template.setStartTime(request.startTime());
        template.setEndTime(request.endTime());
        template.setActive(request.isActive() == null ? true : request.isActive());

        return toTemplateResponse(weeklyTemplateRepository.save(template));
    }

    @Transactional
    public WeeklyTemplateResponseDto updateTemplate(UUID therapistId, UUID templateId, WeeklyTemplateRequestDto request) {
        WeeklyTemplate template = weeklyTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found for id: " + templateId));

        if (!template.getTherapist().getTherapistId().equals(therapistId)) {
            throw new ResourceNotFoundException(
                    "Template " + templateId + " does not belong to therapist " + therapistId);
        }

        validateTemplateWindow(request);

        template.setDayOfWeek(request.dayOfWeek());
        template.setStartTime(request.startTime());
        template.setEndTime(request.endTime());
        if (request.isActive() != null) {
            template.setActive(request.isActive());
        }
        return toTemplateResponse(weeklyTemplateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(UUID therapistId, UUID templateId) {
        WeeklyTemplate template = weeklyTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found for id: " + templateId));
        if (!template.getTherapist().getTherapistId().equals(therapistId)) {
            throw new ResourceNotFoundException(
                    "Template " + templateId + " does not belong to therapist " + therapistId);
        }
        weeklyTemplateRepository.delete(template);
    }

    private void validateTemplateWindow(WeeklyTemplateRequestDto request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new SlotConflictException("endTime must be after startTime.");
        }
    }

    private WeeklyTemplateResponseDto toTemplateResponse(WeeklyTemplate template) {
        return new WeeklyTemplateResponseDto(
                template.getTemplateId(),
                template.getTherapist().getTherapistId(),
                template.getDayOfWeek(),
                template.getStartTime(),
                template.getEndTime(),
                template.isActive()
        );
    }
}
