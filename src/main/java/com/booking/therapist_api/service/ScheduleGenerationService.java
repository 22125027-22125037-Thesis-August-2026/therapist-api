package com.booking.therapist_api.service;

import com.booking.therapist_api.entity.ScheduleSlot;
import com.booking.therapist_api.entity.WeeklyTemplate;
import com.booking.therapist_api.repository.ScheduleSlotRepository;
import com.booking.therapist_api.repository.WeeklyTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleGenerationService.class);
    private static final ZoneId TEMPLATE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int GENERATION_WINDOW_DAYS = 30;

    private final WeeklyTemplateRepository weeklyTemplateRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;

    public ScheduleGenerationService(
            WeeklyTemplateRepository weeklyTemplateRepository,
            ScheduleSlotRepository scheduleSlotRepository
    ) {
        this.weeklyTemplateRepository = weeklyTemplateRepository;
        this.scheduleSlotRepository = scheduleSlotRepository;
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * SUN", zone = "Asia/Ho_Chi_Minh")
    public void generateSlotsForNext30Days() {
        List<WeeklyTemplate> activeTemplates = weeklyTemplateRepository.findByIsActiveTrue();
        LocalDate fromDate = LocalDate.now(TEMPLATE_ZONE);
        LocalDate toDate = fromDate.plusDays(GENERATION_WINDOW_DAYS - 1L);

        int generatedCount = 0;
        for (WeeklyTemplate template : activeTemplates) {
            List<ScheduleSlot> newSlots = new ArrayList<>();
            for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
                if (!date.getDayOfWeek().equals(template.getDayOfWeek())) {
                    continue;
                }

                LocalDateTime localStartDateTime = LocalDateTime.of(date, template.getStartTime());
                LocalDateTime localEndDateTime = LocalDateTime.of(date, template.getEndTime());
                if (!template.getEndTime().isAfter(template.getStartTime())) {
                    localEndDateTime = localEndDateTime.plusDays(1);
                }

                Instant utcStart = localStartDateTime.atZone(TEMPLATE_ZONE).toInstant();
                Instant utcEnd = localEndDateTime.atZone(TEMPLATE_ZONE).toInstant();

                boolean slotExists = scheduleSlotRepository
                        .existsByTherapist_TherapistIdAndStartDatetimeAndEndDatetime(
                                template.getTherapist().getTherapistId(),
                                utcStart,
                                utcEnd
                        );
                if (slotExists) {
                    continue;
                }

                ScheduleSlot slot = new ScheduleSlot();
                slot.setTherapist(template.getTherapist());
                slot.setStartDatetime(utcStart);
                slot.setEndDatetime(utcEnd);
                slot.setBooked(false);
                newSlots.add(slot);
            }

            if (!newSlots.isEmpty()) {
                scheduleSlotRepository.saveAll(newSlots);
                generatedCount += newSlots.size();
            }
        }

        LOGGER.info("Slot generation job finished. activeTemplates={}, generatedSlots={}", activeTemplates.size(), generatedCount);
    }

    @Transactional
    @Scheduled(cron = "0 0 3 1 * *", zone = "Asia/Ho_Chi_Minh")
    public void cleanupOldUnbookedSlots() {
        Instant threshold = Instant.now().minusSeconds(30L * 24 * 60 * 60);
        int deletedCount = scheduleSlotRepository.deleteByIsBookedFalseAndEndDatetimeBefore(threshold);
        LOGGER.info("Schedule slot cleanup job finished. deletedSlots={}, threshold={}", deletedCount, threshold);
    }
}
