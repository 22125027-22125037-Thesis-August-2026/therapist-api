package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

	@Query("SELECT COUNT(DISTINCT a.profileId) FROM Appointment a WHERE a.therapist.therapistId = :therapistId")
	long countDistinctPatientsByTherapistId(@Param("therapistId") UUID therapistId);
}
