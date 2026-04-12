package com.booking.therapist_api.service;

import com.booking.therapist_api.config.RabbitMQConfig;
import com.booking.therapist_api.dto.MatchingPreferenceRequest;
import com.booking.therapist_api.dto.TherapistMatchResponse;
import com.booking.therapist_api.event.CrisisAlertEvent;
import com.booking.therapist_api.event.IntakeMoodLoggedEvent;
import com.booking.therapist_api.event.ProfileDemographicsUpdatedEvent;
import com.booking.therapist_api.entity.ProfileMatchingPreference;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.entity.TherapistAssignment;
import com.booking.therapist_api.enums.AssignmentStatus;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.ProfileMatchingPreferenceRepository;
import com.booking.therapist_api.repository.TherapistAssignmentRepository;
import com.booking.therapist_api.repository.TherapistRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final RabbitTemplate rabbitTemplate;

    public TherapistMatchingService(
            ProfileMatchingPreferenceRepository preferenceRepository,
            TherapistRepository therapistRepository,
            TherapistAssignmentRepository assignmentRepository,
            RabbitTemplate rabbitTemplate
    ) {
        this.preferenceRepository = preferenceRepository;
        this.therapistRepository = therapistRepository;
        this.assignmentRepository = assignmentRepository;
        this.rabbitTemplate = rabbitTemplate;
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

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.BOOKING_EXCHANGE,
            RabbitMQConfig.PROFILE_DEMOGRAPHICS_UPDATED_ROUTING_KEY,
            new ProfileDemographicsUpdatedEvent(profileId, parseAge(request.age()), request.gender())
        );

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.BOOKING_EXCHANGE,
            RabbitMQConfig.TRACKING_MOOD_LOGGED_ROUTING_KEY,
            new IntakeMoodLoggedEvent(profileId, request.moodLevels(), Instant.now())
        );

        if (isCrisisAlertRequested(request.selfHarmThought())) {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.BOOKING_EXCHANGE,
                RabbitMQConfig.AI_CRISIS_ALERTED_ROUTING_KEY,
                new CrisisAlertEvent(profileId, "INTAKE_FORM", "SELF_HARM_THOUGHT_DECLARED", Instant.now())
            );
        }
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

    private Integer parseAge(String age) {
        if (age == null || age.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(age.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isCrisisAlertRequested(String selfHarmThought) {
        if (selfHarmThought == null) {
            return false;
        }
        String normalized = selfHarmThought.trim();
        return "Có".equalsIgnoreCase(normalized) || "Yes".equalsIgnoreCase(normalized);
    }
}
