package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.InternalAssignmentDto;
import com.booking.therapist_api.dto.InternalAssignmentPageDto;
import com.booking.therapist_api.entity.TherapistAssignment;
import com.booking.therapist_api.repository.TherapistAssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class InternalAssignmentService {

    private final TherapistAssignmentRepository assignmentRepository;

    public InternalAssignmentService(TherapistAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public InternalAssignmentPageDto listAssignments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TherapistAssignment> result = assignmentRepository.findAllForReconciliation(pageable);

        return new InternalAssignmentPageDto(
                result.getContent().stream().map(this::toDto).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private InternalAssignmentDto toDto(TherapistAssignment assignment) {
        // The last state-change moment: unassignment time when present, otherwise
        // the original assignment time. Downstream uses this to order updates.
        Instant updatedAt = assignment.getUnassignedAt() != null
                ? assignment.getUnassignedAt()
                : assignment.getAssignedAt();

        return new InternalAssignmentDto(
                assignment.getTherapist().getTherapistId(),
                assignment.getProfileId(),
                assignment.getStatus().name(),
                assignment.getAssignedAt(),
                updatedAt
        );
    }
}
