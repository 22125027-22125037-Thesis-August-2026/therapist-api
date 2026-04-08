package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.ScheduleSlot;
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

    long countByTherapist_TherapistId(UUID therapistId);

    boolean existsByTherapist_TherapistIdAndStartDatetimeAndEndDatetime(
            UUID therapistId,
            Instant startDatetime,
            Instant endDatetime
    );

    @Transactional
    @Modifying
    int deleteByIsBookedFalseAndEndDatetimeBefore(Instant threshold);

    @Transactional
    @Modifying
    @Query(value = "UPDATE schedule_slots SET is_booked = true WHERE slot_id = :slotId AND is_booked = false", nativeQuery = true)
    int lockAndBookSlot(@Param("slotId") UUID slotId);
}
