package com.booking.therapist_api.service;

import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.messaging.TherapistProfileSnapshot;
import com.booking.therapist_api.repository.TherapistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TherapistProfileReplicaServiceTest {

    @Mock
    private TherapistRepository therapistRepository;

    @InjectMocks
    private TherapistProfileReplicaService replicaService;

    @Test
    void applyEvent_updatesAuthOwnedFields_andLeavesTherapistApiOwnedFieldsUntouched() {
        UUID therapistId = UUID.randomUUID();
        Therapist therapist = therapistWithApiOwnedData(therapistId);
        therapist.setFullName("Old Name");
        therapist.setSpecialization("Old Spec");
        therapist.setAboutMe("Old bio");

        when(therapistRepository.findById(therapistId)).thenReturn(Optional.of(therapist));

        Instant occurredAt = Instant.parse("2026-06-02T10:00:00Z");
        TherapistProfileSnapshot snapshot = new TherapistProfileSnapshot(
                therapistId, "New Name", "New Spec", "New bio", 12, "FEMALE", "https://lic/new");

        replicaService.applyEvent(snapshot, occurredAt);

        // auth-owned columns mirrored
        assertEquals("New Name", therapist.getFullName());
        assertEquals("New Spec", therapist.getSpecialization());
        assertEquals("New bio", therapist.getAboutMe());
        assertEquals(12, therapist.getYearsExperience());
        assertEquals("FEMALE", therapist.getGender());
        assertEquals("https://lic/new", therapist.getLicenseUrl());
        // watermark advanced
        assertEquals(occurredAt, therapist.getLastProfileEventAt());

        // therapist-api-owned columns untouched
        assertEquals(new BigDecimal("4.80"), therapist.getRatingAvg());
        assertEquals("empathetic", therapist.getCommunicationStyle());
        assertEquals(Boolean.TRUE, therapist.getIsLgbtqAllied());
        assertEquals("anxiety", therapist.getTreatedChallenges()[0]);

        verify(therapistRepository).save(therapist);
    }

    @Test
    void applyEvent_skipsStaleEvent_whenOccurredAtBeforeWatermark() {
        UUID therapistId = UUID.randomUUID();
        Therapist therapist = therapistWithApiOwnedData(therapistId);
        therapist.setFullName("Current Name");
        therapist.setLastProfileEventAt(Instant.parse("2026-06-02T12:00:00Z"));

        when(therapistRepository.findById(therapistId)).thenReturn(Optional.of(therapist));

        TherapistProfileSnapshot stale = new TherapistProfileSnapshot(
                therapistId, "Stale Name", null, null, null, null, null);

        replicaService.applyEvent(stale, Instant.parse("2026-06-02T11:00:00Z"));

        assertEquals("Current Name", therapist.getFullName());
        verify(therapistRepository, never()).save(any());
    }

    @Test
    void applyEvent_isNoOp_forUnknownTherapist() {
        UUID therapistId = UUID.randomUUID();
        when(therapistRepository.findById(therapistId)).thenReturn(Optional.empty());

        replicaService.applyEvent(
                new TherapistProfileSnapshot(therapistId, "X", null, null, null, null, null),
                Instant.now());

        verify(therapistRepository, never()).save(any());
    }

    @Test
    void reconcile_updatesChangedTherapist_andClearsNullableField() {
        UUID therapistId = UUID.randomUUID();
        Therapist therapist = therapistWithApiOwnedData(therapistId);
        therapist.setFullName("Stale");
        therapist.setSpecialization("Stale Spec");
        therapist.setLicenseUrl("https://lic/old");

        when(therapistRepository.findById(therapistId)).thenReturn(Optional.of(therapist));

        TherapistProfileSnapshot snapshot = new TherapistProfileSnapshot(
                therapistId, "Fresh", "Fresh Spec", null, 5, "MALE", null);

        int updated = replicaService.reconcile(List.of(snapshot));

        assertEquals(1, updated);
        assertEquals("Fresh", therapist.getFullName());
        assertEquals("Fresh Spec", therapist.getSpecialization());
        assertNull(therapist.getLicenseUrl());
        // rating untouched by reconcile
        assertEquals(new BigDecimal("4.80"), therapist.getRatingAvg());
        verify(therapistRepository, times(1)).save(therapist);
    }

    @Test
    void reconcile_isNoOp_whenAlreadyConverged() {
        UUID therapistId = UUID.randomUUID();
        Therapist therapist = therapistWithApiOwnedData(therapistId);
        therapist.setFullName("Same");
        therapist.setSpecialization("Same Spec");
        therapist.setAboutMe("Same bio");
        therapist.setYearsExperience(7);
        therapist.setGender("MALE");
        therapist.setLicenseUrl("https://lic/same");

        when(therapistRepository.findById(therapistId)).thenReturn(Optional.of(therapist));

        TherapistProfileSnapshot snapshot = new TherapistProfileSnapshot(
                therapistId, "Same", "Same Spec", "Same bio", 7, "MALE", "https://lic/same");

        int updated = replicaService.reconcile(List.of(snapshot));

        assertEquals(0, updated);
        verify(therapistRepository, never()).save(any());
    }

    private Therapist therapistWithApiOwnedData(UUID therapistId) {
        Therapist therapist = new Therapist();
        therapist.setTherapistId(therapistId);
        therapist.setRatingAvg(new BigDecimal("4.80"));
        therapist.setCommunicationStyle("empathetic");
        therapist.setIsLgbtqAllied(true);
        therapist.setTreatedChallenges(new String[]{"anxiety"});
        return therapist;
    }
}
