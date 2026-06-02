package com.booking.therapist_api.service;

import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.messaging.TherapistProfileSnapshot;
import com.booking.therapist_api.repository.TherapistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Keeps the {@code therapists} read replica in sync with auth-service's therapist profiles.
 *
 * <p>Mirrors ONLY the auth-owned columns ({@code full_name, specialization, about_me,
 * years_experience, license_url}). Never touches therapist-api-owned data
 * ({@code rating_avg, communication_style, treated_challenges, is_lgbtq_allied, gender}, slots, zoom).
 */
@Service
public class TherapistProfileReplicaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TherapistProfileReplicaService.class);

    private final TherapistRepository therapistRepository;

    public TherapistProfileReplicaService(TherapistRepository therapistRepository) {
        this.therapistRepository = therapistRepository;
    }

    /**
     * Applies a single {@code therapist.profile.updated} event. Idempotent: the change is applied
     * only when {@code occurredAt >= } the stored watermark, so out-of-order or replayed events are
     * dropped. A therapist not present locally is a no-op (nightly reconcile will heal if needed).
     */
    @Transactional
    public void applyEvent(TherapistProfileSnapshot snapshot, Instant occurredAt) {
        Optional<Therapist> existing = therapistRepository.findById(snapshot.profileId());
        if (existing.isEmpty()) {
            LOGGER.info("Ignoring profile event for unknown therapist therapistId={}", snapshot.profileId());
            return;
        }

        Therapist therapist = existing.get();

        Instant watermark = therapist.getLastProfileEventAt();
        if (occurredAt != null && watermark != null && occurredAt.isBefore(watermark)) {
            LOGGER.debug(
                    "Skipping stale profile event therapistId={} occurredAt={} < watermark={}",
                    snapshot.profileId(), occurredAt, watermark);
            return;
        }

        boolean changed = copyAuthOwnedFields(therapist, snapshot);

        // Advance the watermark even on a no-op payload so replays of the same event short-circuit.
        if (occurredAt != null && !occurredAt.equals(watermark)) {
            therapist.setLastProfileEventAt(occurredAt);
            changed = true;
        }

        if (changed) {
            therapistRepository.save(therapist);
            LOGGER.info("Applied profile event therapistId={} occurredAt={}", snapshot.profileId(), occurredAt);
        }
    }

    /**
     * Reconciliation upsert from the auth-service snapshot. Updates only therapists that exist
     * locally and only when an auth-owned field actually differs, so a converged run writes nothing.
     * The watermark is left untouched (the snapshot carries no per-row occurredAt).
     *
     * @return number of therapists actually updated
     */
    @Transactional
    public int reconcile(List<TherapistProfileSnapshot> snapshots) {
        int updated = 0;
        for (TherapistProfileSnapshot snapshot : snapshots) {
            Optional<Therapist> existing = therapistRepository.findById(snapshot.profileId());
            if (existing.isEmpty()) {
                continue;
            }
            Therapist therapist = existing.get();
            if (copyAuthOwnedFields(therapist, snapshot)) {
                therapistRepository.save(therapist);
                updated++;
            }
        }
        return updated;
    }

    /**
     * Copies the auth-owned fields onto the therapist. Returns {@code true} if anything changed.
     * {@code fullName} is the only non-null column, so a null in the snapshot is ignored for it;
     * the remaining fields mirror auth-service exactly, including clearing to null.
     */
    private boolean copyAuthOwnedFields(Therapist therapist, TherapistProfileSnapshot snapshot) {
        boolean changed = false;

        if (snapshot.fullName() != null && !Objects.equals(therapist.getFullName(), snapshot.fullName())) {
            therapist.setFullName(snapshot.fullName());
            changed = true;
        }
        if (!Objects.equals(therapist.getSpecialization(), snapshot.specialization())) {
            therapist.setSpecialization(snapshot.specialization());
            changed = true;
        }
        if (!Objects.equals(therapist.getAboutMe(), snapshot.bio())) {
            therapist.setAboutMe(snapshot.bio());
            changed = true;
        }
        if (!Objects.equals(therapist.getYearsExperience(), snapshot.yearsExperience())) {
            therapist.setYearsExperience(snapshot.yearsExperience());
            changed = true;
        }
        // gender is intentionally NOT mirrored: it isn't editable in auth-service
        // (ProfileUpdateRequest has no gender) and auth stores it null for therapists, so mirroring
        // would blank therapist-api's own seeded gender. It stays therapist-api-owned.
        if (!Objects.equals(therapist.getLicenseUrl(), snapshot.licenseUrl())) {
            therapist.setLicenseUrl(snapshot.licenseUrl());
            changed = true;
        }

        return changed;
    }
}
