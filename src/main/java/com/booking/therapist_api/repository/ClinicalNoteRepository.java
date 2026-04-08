package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, UUID> {

    boolean existsByAppointment_Id(UUID appointmentId);
}
