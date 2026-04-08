package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.ReviewRequestDto;
import com.booking.therapist_api.dto.ReviewResponseDto;
import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.entity.Review;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.enums.AppointmentStatus;
import com.booking.therapist_api.exception.InvalidAppointmentStateException;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.exception.ReviewAlreadyExistsException;
import com.booking.therapist_api.exception.ReviewNotAllowedException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ReviewRepository;
import com.booking.therapist_api.repository.TherapistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class ReviewService {

    private final AppointmentRepository appointmentRepository;
    private final ReviewRepository reviewRepository;
    private final TherapistRepository therapistRepository;

    public ReviewService(
            AppointmentRepository appointmentRepository,
            ReviewRepository reviewRepository,
            TherapistRepository therapistRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.reviewRepository = reviewRepository;
        this.therapistRepository = therapistRepository;
    }

    @Transactional
    public ReviewResponseDto submitReview(UUID patientId, ReviewRequestDto request) {
        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found for id: " + request.appointmentId()));

        if (!appointment.getProfileId().equals(patientId)) {
            throw new ReviewNotAllowedException(
                    "You can only review your own completed appointment.");
        }

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new InvalidAppointmentStateException(
                    "Review can only be submitted when appointment is COMPLETED. Current status: "
                            + appointment.getStatus().name());
        }

        if (reviewRepository.existsByAppointment_Id(appointment.getId())) {
            throw new ReviewAlreadyExistsException(
                    "Review already exists for appointment id: " + appointment.getId());
        }

        Review review = new Review();
        review.setAppointment(appointment);
        review.setRating(request.rating());
        review.setComment(request.comment());

        Review savedReview = reviewRepository.save(review);

        Therapist therapist = appointment.getTherapist();
        Double averageRating = reviewRepository.findAverageRatingByTherapistId(therapist.getTherapistId());
        BigDecimal ratingAvg = averageRating == null
                ? null
                : BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP);

        therapist.setRatingAvg(ratingAvg);
        therapistRepository.save(therapist);

        return new ReviewResponseDto(
                savedReview.getReviewId(),
                appointment.getId(),
                therapist.getTherapistId(),
                savedReview.getRating(),
                ratingAvg,
                savedReview.getCreatedAt(),
                "Review submitted successfully"
        );
    }
}
