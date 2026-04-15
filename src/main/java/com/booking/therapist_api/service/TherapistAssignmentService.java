package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.ActiveAssignedTherapistResponse;
import com.booking.therapist_api.dto.AssignedTherapistSummary;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.entity.TherapistAssignment;
import com.booking.therapist_api.enums.AssignmentStatus;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.TherapistAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TherapistAssignmentService {

    private final TherapistAssignmentRepository assignmentRepository;

    public TherapistAssignmentService(TherapistAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public ActiveAssignedTherapistResponse getActiveAssignedTherapist(UUID profileId) {
        TherapistAssignment assignment = assignmentRepository.findByProfileIdAndStatus(profileId, AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active therapist assignment not found for profile id: " + profileId));

        Therapist therapist = assignment.getTherapist();

        return new ActiveAssignedTherapistResponse(
                assignment.getAssignmentId(),
                assignment.getProfileId(),
                assignment.getStatus().name(),
                assignment.getAssignedAt(),
                new AssignedTherapistSummary(
                        therapist.getTherapistId(),
                        therapist.getFullName(),
                        therapist.getSpecialization(),
                        therapist.getCommunicationStyle()
                )
        );
    }
}
