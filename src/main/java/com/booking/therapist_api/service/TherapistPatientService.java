package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.PatientMatchingPreferenceResponseDto;
import com.booking.therapist_api.dto.PatientRiskLevelRequestDto;
import com.booking.therapist_api.dto.PatientRiskLevelResponseDto;
import com.booking.therapist_api.dto.PatientTagsRequestDto;
import com.booking.therapist_api.dto.PatientTagsResponseDto;
import com.booking.therapist_api.dto.TherapistPatientItemDto;
import com.booking.therapist_api.entity.PatientTag;
import com.booking.therapist_api.entity.ProfileMatchingPreference;
import com.booking.therapist_api.entity.TherapistAssignment;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.PatientTagRepository;
import com.booking.therapist_api.repository.ProfileMatchingPreferenceRepository;
import com.booking.therapist_api.repository.TherapistAssignmentRepository;
import com.booking.therapist_api.repository.TherapistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TherapistPatientService {

    private final TherapistRepository therapistRepository;
    private final TherapistAssignmentRepository assignmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfileMatchingPreferenceRepository preferenceRepository;
    private final PatientTagRepository patientTagRepository;

    public TherapistPatientService(
            TherapistRepository therapistRepository,
            TherapistAssignmentRepository assignmentRepository,
            AppointmentRepository appointmentRepository,
            ProfileMatchingPreferenceRepository preferenceRepository,
            PatientTagRepository patientTagRepository
    ) {
        this.therapistRepository = therapistRepository;
        this.assignmentRepository = assignmentRepository;
        this.appointmentRepository = appointmentRepository;
        this.preferenceRepository = preferenceRepository;
        this.patientTagRepository = patientTagRepository;
    }

    // §4.1 — list every profile assigned (active or historical) to this therapist.
    @Transactional(readOnly = true)
    public List<TherapistPatientItemDto> listPatients(UUID therapistId) {
        if (!therapistRepository.existsById(therapistId)) {
            throw new ResourceNotFoundException("Therapist not found for id: " + therapistId);
        }

        List<TherapistAssignment> assignments =
                assignmentRepository.findAllByTherapist_TherapistIdOrderByAssignedAtDesc(therapistId);

        Map<UUID, String> nameByProfile = buildPatientNameIndex(therapistId);
        Map<UUID, PatientTag> tagByProfile = new HashMap<>();
        for (PatientTag t : patientTagRepository.findAll()) {
            tagByProfile.put(t.getProfileId(), t);
        }

        List<TherapistPatientItemDto> items = new ArrayList<>(assignments.size());
        for (TherapistAssignment a : assignments) {
            PatientTag tag = tagByProfile.get(a.getProfileId());
            items.add(new TherapistPatientItemDto(
                    a.getProfileId(),
                    nameByProfile.get(a.getProfileId()),
                    a.getStatus().name(),
                    a.getAssignedAt(),
                    a.getUnassignedAt(),
                    tag != null && tag.getRiskLevel() != null ? tag.getRiskLevel().name() : null,
                    tag != null && tag.getTags() != null ? Arrays.asList(tag.getTags()) : null
            ));
        }
        return items;
    }

    private Map<UUID, String> buildPatientNameIndex(UUID therapistId) {
        Map<UUID, String> nameByProfile = new HashMap<>();
        appointmentRepository.findTherapistAppointments(
                therapistId,
                null,
                null,
                null,
                org.springframework.data.domain.Pageable.unpaged()
        ).forEach(appt -> {
            if (appt.getPatientName() != null && !appt.getPatientName().isBlank()) {
                nameByProfile.putIfAbsent(appt.getProfileId(), appt.getPatientName());
            }
        });
        return nameByProfile;
    }

    // §4.3 — therapist reads the patient's matching-preference responses.
    @Transactional(readOnly = true)
    public PatientMatchingPreferenceResponseDto getMatchingPreferences(UUID profileId) {
        ProfileMatchingPreference pref = preferenceRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Matching preference not found for profile id: " + profileId));

        List<String> reasons = pref.getReasons() == null ? null : Arrays.asList(pref.getReasons());
        return new PatientMatchingPreferenceResponseDto(
                pref.getProfileId(),
                pref.getHasPriorCounseling(),
                pref.getSexualOrientation(),
                pref.getIsLgbtqPriority(),
                reasons,
                pref.getCommunicationStyle(),
                pref.getLastUpdatedAt()
        );
    }

    // §4.4 — tags
    @Transactional(readOnly = true)
    public PatientTagsResponseDto getTags(UUID profileId) {
        return patientTagRepository.findById(profileId)
                .map(t -> new PatientTagsResponseDto(
                        t.getProfileId(),
                        t.getTags() == null ? List.of() : Arrays.asList(t.getTags()),
                        t.getUpdatedAt(),
                        t.getUpdatedBy()
                ))
                .orElseGet(() -> new PatientTagsResponseDto(profileId, List.of(), null, null));
    }

    @Transactional
    public PatientTagsResponseDto upsertTags(UUID profileId, UUID updatedBy, PatientTagsRequestDto request) {
        PatientTag tag = patientTagRepository.findById(profileId)
                .orElseGet(() -> {
                    PatientTag fresh = new PatientTag();
                    fresh.setProfileId(profileId);
                    return fresh;
                });
        String[] tagsArray = request.tags() == null
                ? new String[0]
                : request.tags().toArray(new String[0]);
        tag.setTags(tagsArray);
        tag.setUpdatedBy(updatedBy);
        PatientTag saved = patientTagRepository.save(tag);
        return new PatientTagsResponseDto(
                saved.getProfileId(),
                Arrays.asList(saved.getTags()),
                saved.getUpdatedAt(),
                saved.getUpdatedBy()
        );
    }

    // §4.4 — risk level
    @Transactional(readOnly = true)
    public PatientRiskLevelResponseDto getRiskLevel(UUID profileId) {
        Optional<PatientTag> maybe = patientTagRepository.findById(profileId);
        if (maybe.isEmpty()) {
            return new PatientRiskLevelResponseDto(profileId, null, null, null);
        }
        PatientTag t = maybe.get();
        return new PatientRiskLevelResponseDto(
                t.getProfileId(),
                t.getRiskLevel() == null ? null : t.getRiskLevel().name(),
                t.getUpdatedAt(),
                t.getUpdatedBy()
        );
    }

    @Transactional
    public PatientRiskLevelResponseDto upsertRiskLevel(
            UUID profileId,
            UUID updatedBy,
            PatientRiskLevelRequestDto request
    ) {
        PatientTag tag = patientTagRepository.findById(profileId)
                .orElseGet(() -> {
                    PatientTag fresh = new PatientTag();
                    fresh.setProfileId(profileId);
                    return fresh;
                });
        tag.setRiskLevel(request.riskLevel());
        tag.setUpdatedBy(updatedBy);
        PatientTag saved = patientTagRepository.save(tag);
        return new PatientRiskLevelResponseDto(
                saved.getProfileId(),
                saved.getRiskLevel().name(),
                saved.getUpdatedAt(),
                saved.getUpdatedBy()
        );
    }
}
