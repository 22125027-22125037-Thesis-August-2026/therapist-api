package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.TherapistAssignment;
import com.booking.therapist_api.enums.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TherapistAssignmentRepository extends JpaRepository<TherapistAssignment, UUID> {

    Optional<TherapistAssignment> findByProfileIdAndStatus(UUID profileId, AssignmentStatus status);

    /**
     * Stable, fully ordered page of every assignment for reconciliation. Ordering
     * by (assignedAt, assignmentId) guarantees a deterministic page boundary even
     * when multiple rows share the same assignedAt timestamp.
     */
    @Query(value = "SELECT a FROM TherapistAssignment a JOIN FETCH a.therapist "
            + "ORDER BY a.assignedAt ASC, a.assignmentId ASC",
            countQuery = "SELECT COUNT(a) FROM TherapistAssignment a")
    Page<TherapistAssignment> findAllForReconciliation(Pageable pageable);

    List<TherapistAssignment> findAllByTherapist_TherapistIdAndStatus(UUID therapistId, AssignmentStatus status);

    List<TherapistAssignment> findAllByTherapist_TherapistIdOrderByAssignedAtDesc(UUID therapistId);

    List<TherapistAssignment> findAllByProfileId(UUID profileId);
}
