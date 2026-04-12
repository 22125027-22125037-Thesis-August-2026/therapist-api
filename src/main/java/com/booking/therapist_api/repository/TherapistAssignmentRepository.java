package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.TherapistAssignment;
import com.booking.therapist_api.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TherapistAssignmentRepository extends JpaRepository<TherapistAssignment, UUID> {

    Optional<TherapistAssignment> findByProfileIdAndStatus(UUID profileId, AssignmentStatus status);

    List<TherapistAssignment> findAllByTherapist_TherapistIdAndStatus(UUID therapistId, AssignmentStatus status);
}
