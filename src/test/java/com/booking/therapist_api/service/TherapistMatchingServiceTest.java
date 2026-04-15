package com.booking.therapist_api.service;

import com.booking.therapist_api.config.RabbitMQConfig;
import com.booking.therapist_api.dto.MatchingPreferenceRequest;
import com.booking.therapist_api.dto.TherapistMatchResponse;
import com.booking.therapist_api.entity.ProfileMatchingPreference;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.entity.TherapistAssignment;
import com.booking.therapist_api.enums.AssignmentStatus;
import com.booking.therapist_api.event.IntakeMoodLoggedEvent;
import com.booking.therapist_api.event.ProfileDemographicsUpdatedEvent;
import com.booking.therapist_api.repository.ProfileMatchingPreferenceRepository;
import com.booking.therapist_api.repository.TherapistAssignmentRepository;
import com.booking.therapist_api.repository.TherapistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TherapistMatchingServiceTest {

    @Mock
    private ProfileMatchingPreferenceRepository preferenceRepository;

    @Mock
    private TherapistRepository therapistRepository;

    @Mock
    private TherapistAssignmentRepository assignmentRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private TherapistMatchingService therapistMatchingService;

    @Test
    void savePreferences_autoAssignsTopMatch_usingFallbackWhenStyleUnknown() {
        UUID profileId = UUID.randomUUID();

        MatchingPreferenceRequest request = new MatchingPreferenceRequest(
                "never",
                "male",
                "22",
                "gay",
                true,
                "no",
                List.of("anxiety", "depression"),
                Map.of("anxiety", 5, "lossInterest", 4, "fatigue", 4),
                "listener"
        );

        Therapist topMatch = createTherapist("empathetic", true, new String[]{"anxiety", "depression"});

        when(preferenceRepository.findById(profileId)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(ProfileMatchingPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(therapistRepository.findMatchingTherapists(eq(true), eq("listener"), any(String[].class)))
                .thenReturn(List.of());
        when(therapistRepository.findMatchingTherapists(eq(true), isNull(), any(String[].class)))
                .thenReturn(List.of(topMatch));
        when(assignmentRepository.findByProfileIdAndStatus(profileId, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        therapistMatchingService.savePreferences(profileId, request);

        ArgumentCaptor<TherapistAssignment> assignmentCaptor = ArgumentCaptor.forClass(TherapistAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());

        TherapistAssignment savedAssignment = assignmentCaptor.getValue();
        assertEquals(profileId, savedAssignment.getProfileId());
        assertEquals(AssignmentStatus.ACTIVE, savedAssignment.getStatus());
        assertEquals(topMatch, savedAssignment.getTherapist());

        verify(therapistRepository).findMatchingTherapists(eq(true), eq("listener"), any(String[].class));
        verify(therapistRepository).findMatchingTherapists(eq(true), isNull(), any(String[].class));
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.BOOKING_EXCHANGE),
                eq(RabbitMQConfig.PROFILE_DEMOGRAPHICS_UPDATED_ROUTING_KEY),
                any(ProfileDemographicsUpdatedEvent.class)
        );
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.BOOKING_EXCHANGE),
                eq(RabbitMQConfig.TRACKING_MOOD_LOGGED_ROUTING_KEY),
                any(IntakeMoodLoggedEvent.class)
        );
    }

    @Test
    void findMatches_returnsResultsFromFallbackWhenStyleHasNoExactMatch() {
        UUID profileId = UUID.randomUUID();
        ProfileMatchingPreference preference = new ProfileMatchingPreference();
        preference.setProfileId(profileId);
        preference.setIsLgbtqPriority(true);
        preference.setCommunicationStyle("listener");
        preference.setReasons(new String[]{"anxiety", "depression"});

        Therapist matchedTherapist = createTherapist("supportive", true, new String[]{"depression", "anxiety"});

        when(preferenceRepository.findById(profileId)).thenReturn(Optional.of(preference));
        when(therapistRepository.findMatchingTherapists(eq(true), eq("listener"), any(String[].class)))
                .thenReturn(List.of());
        when(therapistRepository.findMatchingTherapists(eq(true), isNull(), any(String[].class)))
                .thenReturn(List.of(matchedTherapist));

        List<TherapistMatchResponse> matches = therapistMatchingService.findMatches(profileId);

        assertEquals(1, matches.size());
        assertNotNull(matches.get(0).id());
        assertEquals("Fallback Therapist", matches.get(0).fullName());
        assertEquals(new BigDecimal("2"), matches.get(0).matchScore());

        verify(therapistRepository).findMatchingTherapists(eq(true), eq("listener"), any(String[].class));
        verify(therapistRepository).findMatchingTherapists(eq(true), isNull(), any(String[].class));
    }

    private Therapist createTherapist(String communicationStyle, boolean isLgbtqAllied, String[] treatedChallenges) {
        Therapist therapist = new Therapist();
        therapist.setTherapistId(UUID.randomUUID());
        therapist.setAccountId(UUID.randomUUID());
        therapist.setFullName("Fallback Therapist");
        therapist.setSpecialization("Mood disorders");
        therapist.setCommunicationStyle(communicationStyle);
        therapist.setIsLgbtqAllied(isLgbtqAllied);
        therapist.setTreatedChallenges(treatedChallenges);
        therapist.setRatingAvg(new BigDecimal("4.90"));
        return therapist;
    }
}
