package com.booking.therapist_api.repository;

import com.booking.therapist_api.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByAppointment_Id(UUID appointmentId);

    List<Review> findByAppointment_Therapist_TherapistIdOrderByCreatedAtDesc(UUID therapistId);

    long countByAppointment_Therapist_TherapistId(UUID therapistId);

    @Query("SELECT AVG(r.rating) FROM Review r JOIN r.appointment a WHERE a.therapist.therapistId = :therapistId")
    Double findAverageRatingByTherapistId(@Param("therapistId") UUID therapistId);
}
