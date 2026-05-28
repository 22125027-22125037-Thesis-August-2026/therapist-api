package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.ClinicalNote;
import com.booking.therapist_api.enums.ClinicalNoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, UUID> {

    boolean existsByAppointment_Id(UUID appointmentId);

    Optional<ClinicalNote> findByAppointment_Id(UUID appointmentId);

    @Query("""
            SELECT n
            FROM ClinicalNote n
            WHERE (:therapistId IS NULL OR n.appointment.therapist.therapistId = :therapistId)
              AND (:patientId   IS NULL OR n.appointment.profileId            = :patientId)
              AND (:status      IS NULL OR n.status                           = :status)
            ORDER BY n.updatedAt DESC
        """)
    Page<ClinicalNote> search(
            @Param("therapistId") UUID therapistId,
            @Param("patientId") UUID patientId,
            @Param("status") ClinicalNoteStatus status,
            Pageable pageable
    );

    long countByAppointment_Therapist_TherapistIdAndStatus(UUID therapistId, ClinicalNoteStatus status);
}
