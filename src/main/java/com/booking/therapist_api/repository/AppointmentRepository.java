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

	@Query("""
			SELECT a
			FROM Appointment a
			WHERE a.profileId = :profileId
			  AND a.status IN :statuses
			  AND a.startDatetime >= :recentCutoff
			ORDER BY a.startDatetime ASC
		""")
	Optional<Appointment> findClosestUpcomingOrRecentInProgress(
			@Param("profileId") UUID profileId,
			@Param("statuses") Collection<AppointmentStatus> statuses,
			@Param("recentCutoff") Instant recentCutoff
	);

	List<Appointment> findByProfileIdAndStatusInOrderByStartDatetimeDesc(
			UUID profileId,
			Collection<AppointmentStatus> statuses
	);

	List<Appointment> findByProfileIdAndStatusAndReviewIsNullOrderByStartDatetimeDesc(
			UUID profileId,
			AppointmentStatus status
	);
}
