package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.WeeklyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WeeklyTemplateRepository extends JpaRepository<WeeklyTemplate, UUID> {

    List<WeeklyTemplate> findByIsActiveTrue();

    List<WeeklyTemplate> findByTherapist_TherapistIdAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(UUID therapistId);
}
