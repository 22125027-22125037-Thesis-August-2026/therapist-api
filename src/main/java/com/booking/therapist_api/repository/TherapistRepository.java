package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TherapistRepository extends JpaRepository<Therapist, UUID> {

	@Query(value = """
			SELECT t.*
			FROM therapists t
			WHERE (:communicationStyle IS NULL OR LOWER(t.communication_style) = LOWER(:communicationStyle))
			  AND (:isLgbtqPriority = false OR t.is_lgbtq_allied = true)
			ORDER BY (
				SELECT COUNT(*)
				FROM (
					SELECT UNNEST(t.treated_challenges)
					INTERSECT
					SELECT UNNEST(CAST(:reasons AS varchar[]))
				) overlap_items
			) DESC,
			t.rating_avg DESC NULLS LAST,
			t.therapist_id ASC
			""", nativeQuery = true)
	List<Therapist> findMatchingTherapists(
			@Param("isLgbtqPriority") boolean isLgbtqPriority,
			@Param("communicationStyle") String communicationStyle,
			@Param("reasons") String[] reasons
	);
}
