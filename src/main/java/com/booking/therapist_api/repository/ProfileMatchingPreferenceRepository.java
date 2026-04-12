package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.ProfileMatchingPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProfileMatchingPreferenceRepository extends JpaRepository<ProfileMatchingPreference, UUID> {
}
