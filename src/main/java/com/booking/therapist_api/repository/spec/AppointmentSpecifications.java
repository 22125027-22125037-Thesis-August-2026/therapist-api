package com.booking.therapist_api.repository.spec;

import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.enums.AppointmentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Specifications for {@link Appointment} queries with optional filters.
 *
 * Why this exists rather than a JPQL {@code @Query} with {@code (:from IS NULL OR ...)}
 * placeholders: Hibernate sends those queries to Postgres with typeless {@code ?}
 * parameters in {@code ? IS NULL} positions, and Postgres rejects them at prepare
 * time with "could not determine data type of parameter". Building the criteria
 * dynamically means the optional clauses simply don't appear in the SQL when the
 * caller passes null, so there is no typeless placeholder for Postgres to choke on.
 */
public final class AppointmentSpecifications {

    private AppointmentSpecifications() {
    }

    /**
     * Appointments owned by {@code therapistId}, optionally filtered by status and a
     * {@code [from, to)} window over {@code startDatetime}. Ordering is left to the
     * caller's {@link org.springframework.data.domain.Pageable} so the spec stays
     * safe to use with count queries.
     */
    public static Specification<Appointment> ofTherapist(
            UUID therapistId,
            Collection<AppointmentStatus> statuses,
            Instant from,
            Instant to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("therapist").get("therapistId"), therapistId));

            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDatetime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("startDatetime"), to));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
