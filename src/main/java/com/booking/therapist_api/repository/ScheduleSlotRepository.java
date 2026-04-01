package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, UUID> {

    @Transactional
    @Modifying
    @Query(value = "UPDATE schedule_slots SET is_booked = true WHERE id = :slotId AND is_booked = false", nativeQuery = true)
    int lockAndBookSlot(@Param("slotId") UUID slotId);
}
