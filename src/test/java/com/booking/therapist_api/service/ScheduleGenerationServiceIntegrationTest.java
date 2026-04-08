package com.booking.therapist_api.service;

import com.booking.therapist_api.entity.ScheduleSlot;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.entity.WeeklyTemplate;
import com.booking.therapist_api.repository.ScheduleSlotRepository;
import com.booking.therapist_api.repository.WeeklyTemplateRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleGenerationServiceIntegrationTest {

    private static final ZoneId TEMPLATE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private ScheduleGenerationService scheduleGenerationService;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private WeeklyTemplateRepository weeklyTemplateRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID therapistId;

    @BeforeEach
    void setUp() {
        Therapist therapist = new Therapist();
        therapist.setAccountId(UUID.randomUUID());
        therapist.setFullName("Test Therapist");
        therapist = entityManager.merge(therapist);
        therapistId = therapist.getTherapistId();

        LocalDate localNow = LocalDate.now(TEMPLATE_ZONE);
        DayOfWeek templateDay = localNow.getDayOfWeek();

        WeeklyTemplate template = new WeeklyTemplate();
        template.setTherapist(therapist);
        template.setDayOfWeek(templateDay);
        template.setStartTime(LocalTime.of(9, 0));
        template.setEndTime(LocalTime.of(10, 0));
        template.setActive(true);
        weeklyTemplateRepository.save(template);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void generateSlots_convertsAsiaHoChiMinhToUtc_andIsIdempotent() {
        LocalDate localDate = LocalDate.now(TEMPLATE_ZONE);
        Instant expectedUtcStart = LocalDateTime.of(localDate, LocalTime.of(9, 0))
                .atZone(TEMPLATE_ZONE)
                .toInstant();
        Instant expectedUtcEnd = LocalDateTime.of(localDate, LocalTime.of(10, 0))
                .atZone(TEMPLATE_ZONE)
                .toInstant();

        scheduleGenerationService.generateSlotsForNext30Days();

        List<ScheduleSlot> generatedSlots = scheduleSlotRepository
                .findByTherapist_TherapistIdOrderByStartDatetimeAsc(therapistId);

        assertTrue(!generatedSlots.isEmpty(), "Expected slots to be generated for the active template");
        assertTrue(
                generatedSlots.stream().anyMatch(slot ->
                        slot.getStartDatetime().equals(expectedUtcStart)
                                && slot.getEndDatetime().equals(expectedUtcEnd)
                ),
                "Expected a generated slot with correctly converted UTC start/end instants"
        );

        long countAfterFirstRun = scheduleSlotRepository.countByTherapist_TherapistId(therapistId);

        scheduleGenerationService.generateSlotsForNext30Days();

        long countAfterSecondRun = scheduleSlotRepository.countByTherapist_TherapistId(therapistId);
        assertEquals(countAfterFirstRun, countAfterSecondRun, "Second generation run must not create duplicates");
    }
}
