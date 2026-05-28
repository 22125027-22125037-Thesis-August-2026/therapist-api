package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.PatientTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientTagRepository extends JpaRepository<PatientTag, UUID> {
}
