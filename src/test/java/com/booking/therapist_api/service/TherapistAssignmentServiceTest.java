package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.ActiveAssignedTherapistResponse;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.entity.TherapistAssignment;
import com.booking.therapist_api.enums.AssignmentStatus;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.TherapistAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TherapistAssignmentServiceTest {

    @Mock
    private TherapistAssignmentRepository assignmentRepository;

    @InjectMocks
    private TherapistAssignmentService therapistAssignmentService;

    @Test
    void getActiveAssignedTherapist_returnsActiveAssignmentDetails() {
        UUID profileId = UUID.randomUUID();

        Therapist therapist = new Therapist();
        therapist.setTherapistId(UUID.randomUUID());
        therapist.setFullName("Nguyen Thi A");
        therapist.setSpecialization("Anxiety & Stress");
        therapist.setCommunicationStyle("empathetic");

        TherapistAssignment assignment = new TherapistAssignment();
        assignment.setAssignmentId(UUID.randomUUID());
        assignment.setProfileId(profileId);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setTherapist(therapist);

        when(assignmentRepository.findByProfileIdAndStatus(profileId, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));

        ActiveAssignedTherapistResponse response = therapistAssignmentService.getActiveAssignedTherapist(profileId);

        assertEquals(assignment.getAssignmentId(), response.assignmentId());
        assertEquals(profileId, response.profileId());
        assertEquals("ACTIVE", response.status());
        assertEquals(therapist.getTherapistId(), response.therapist().id());
        assertEquals(therapist.getFullName(), response.therapist().fullName());
    }

    @Test
    void getActiveAssignedTherapist_throwsWhenNoActiveAssignment() {
        UUID profileId = UUID.randomUUID();

        when(assignmentRepository.findByProfileIdAndStatus(profileId, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> therapistAssignmentService.getActiveAssignedTherapist(profileId));
    }
}
