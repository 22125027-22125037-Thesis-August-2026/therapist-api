package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TherapistRepository extends JpaRepository<Therapist, UUID> {
}
