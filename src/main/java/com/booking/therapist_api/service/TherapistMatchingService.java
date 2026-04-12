package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.MatchingPreferenceRequest;
import com.booking.therapist_api.dto.TherapistMatchResponse;
import com.booking.therapist_api.entity.ProfileMatchingPreference;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.entity.TherapistAssignment;
import com.booking.therapist_api.enums.AssignmentStatus;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.ProfileMatchingPreferenceRepository;
import com.booking.therapist_api.repository.TherapistAssignmentRepository;
import com.booking.therapist_api.repository.TherapistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class TherapistMatchingService {

    private final ProfileMatchingPreferenceRepository preferenceRepository;
    private final TherapistRepository therapistRepository;
    private final TherapistAssignmentRepository assignmentRepository;

    public TherapistMatchingService(
            ProfileMatchingPreferenceRepository preferenceRepository,
            TherapistRepository therapistRepository,
            TherapistAssignmentRepository assignmentRepository
    ) {
        this.preferenceRepository = preferenceRepository;
        this.therapistRepository = therapistRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public void savePreferences(UUID profileId, MatchingPreferenceRequest request) {
        ProfileMatchingPreference preference = preferenceRepository.findById(profileId)
                .orElseGet(ProfileMatchingPreference::new);

        preference.setProfileId(profileId);
        preference.setHasPriorCounseling(request.hasPriorCounseling());
        preference.setSexualOrientation(request.sexualOrientation());
        preference.setIsLgbtqPriority(request.isLgbtqPriority());
        preference.setReasons(request.reasons().toArray(new String[0]));
        preference.setCommunicationStyle(request.communicationStyle());

        preferenceRepository.save(preference);
    }

    @Transactional(readOnly = true)
    public List<TherapistMatchResponse> findMatches(UUID profileId) {
        ProfileMatchingPreference preference = preferenceRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Matching preference not found for profile id: " + profileId));

        String[] requestedReasons = preference.getReasons() == null ? new String[0] : preference.getReasons();
        boolean isLgbtqPriority = Boolean.TRUE.equals(preference.getIsLgbtqPriority());

        List<Therapist> therapists = therapistRepository.findMatchingTherapists(
                isLgbtqPriority,
                preference.getCommunicationStyle(),
                requestedReasons
        );

        return therapists.stream()
                .map(therapist -> toMatchResponse(therapist, requestedReasons))
                .toList();
    }

    @Transactional
    public void assignTherapist(UUID profileId, UUID therapistId) {
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found for id: " + therapistId));

        assignmentRepository.findByProfileIdAndStatus(profileId, AssignmentStatus.ACTIVE)
                .ifPresent(activeAssignment -> {
                    activeAssignment.setStatus(AssignmentStatus.INACTIVE);
                    activeAssignment.setUnassignedAt(Instant.now());
                    assignmentRepository.save(activeAssignment);
                });

        TherapistAssignment assignment = new TherapistAssignment();
        assignment.setProfileId(profileId);
        assignment.setTherapist(therapist);
        assignment.setStatus(AssignmentStatus.ACTIVE);

        assignmentRepository.save(assignment);
    }

    private TherapistMatchResponse toMatchResponse(Therapist therapist, String[] requestedReasons) {
        List<String> matchingReasons = overlappingReasons(requestedReasons, therapist.getTreatedChallenges());

        return new TherapistMatchResponse(
                therapist.getTherapistId(),
                therapist.getFullName(),
                therapist.getSpecialization(),
                BigDecimal.valueOf(matchingReasons.size()),
                matchingReasons,
                therapist.getCommunicationStyle()
        );
    }

    private List<String> overlappingReasons(String[] requestedReasons, String[] therapistChallenges) {
        List<String> overlaps = new ArrayList<>();
        if (requestedReasons == null || therapistChallenges == null) {
            return overlaps;
        }

        List<String> therapistChallengeList = Arrays.asList(therapistChallenges);
        for (String reason : requestedReasons) {
            if (therapistChallengeList.contains(reason)) {
                overlaps.add(reason);
            }
        }
        return overlaps;
    }
}
