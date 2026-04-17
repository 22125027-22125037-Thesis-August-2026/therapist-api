package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

	@Query("SELECT COUNT(DISTINCT a.profileId) FROM Appointment a WHERE a.therapist.therapistId = :therapistId")
	long countDistinctPatientsByTherapistId(@Param("therapistId") UUID therapistId);

	Optional<Appointment> findFirstByProfileIdAndStartDatetimeAfterAndStatusOrderByStartDatetimeAsc(
			UUID profileId,
			Instant startDatetime,
			AppointmentStatus status
	);

	List<Appointment> findByProfileIdAndStatusInOrderByStartDatetimeDesc(
			UUID profileId,
			Collection<AppointmentStatus> statuses
	);
}
