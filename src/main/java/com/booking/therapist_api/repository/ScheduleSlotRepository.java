package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.ScheduleSlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, UUID> {

    List<ScheduleSlot> findByTherapist_TherapistIdOrderByStartDatetimeAsc(UUID therapistId);

    Page<ScheduleSlot> findByTherapist_TherapistIdAndIsBookedFalseAndStartDatetimeAfterOrderByStartDatetimeAsc(
            UUID therapistId,
            Instant now,
            Pageable pageable
    );

    Page<ScheduleSlot> findByTherapist_TherapistIdAndStartDatetimeAfterOrderByStartDatetimeAsc(
            UUID therapistId,
            Instant now,
            Pageable pageable
    );

    long countByTherapist_TherapistId(UUID therapistId);

    boolean existsByTherapist_TherapistIdAndStartDatetimeAndEndDatetime(
            UUID therapistId,
            Instant startDatetime,
            Instant endDatetime
    );

    @Query("""
            SELECT COUNT(s) > 0
            FROM ScheduleSlot s
            WHERE s.therapist.therapistId = :therapistId
              AND s.id <> :excludingSlotId
              AND s.startDatetime < :endDatetime
              AND s.endDatetime > :startDatetime
        """)
    boolean overlapsExistingExcluding(
            @Param("therapistId") UUID therapistId,
            @Param("excludingSlotId") UUID excludingSlotId,
            @Param("startDatetime") Instant startDatetime,
            @Param("endDatetime") Instant endDatetime
    );

    @Query("""
            SELECT COUNT(s) > 0
            FROM ScheduleSlot s
            WHERE s.therapist.therapistId = :therapistId
              AND s.startDatetime < :endDatetime
              AND s.endDatetime > :startDatetime
        """)
    boolean overlapsExisting(
            @Param("therapistId") UUID therapistId,
            @Param("startDatetime") Instant startDatetime,
            @Param("endDatetime") Instant endDatetime
    );

    @Transactional
    @Modifying
    int deleteByIsBookedFalseAndEndDatetimeBefore(Instant threshold);

    @Transactional
    @Modifying
    @Query(value = "UPDATE schedule_slots SET is_booked = true WHERE slot_id = :slotId AND is_booked = false", nativeQuery = true)
    int lockAndBookSlot(@Param("slotId") UUID slotId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE schedule_slots SET is_booked = false WHERE slot_id = :slotId", nativeQuery = true)
    int releaseSlot(@Param("slotId") UUID slotId);
}
