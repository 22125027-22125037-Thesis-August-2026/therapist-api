package com.booking.therapist_api.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Flat snapshot of the auth-owned therapist-profile fields that therapist-api mirrors into the
 * {@code therapists} read replica. Field set matches auth-service's {@code TherapistProfileView},
 * carried both by the {@code therapist.profile.updated} event and the
 * {@code /internal/therapist-profiles} reconciliation endpoint.
 *
 * <p>{@code profileId} equals {@code therapists.therapist_id}. Only the columns auth-service owns
 * are represented here — rating, matching attributes, slots and zoom are intentionally absent.
 */
public record TherapistProfileSnapshot(
        UUID profileId,
        String fullName,
        String specialization,
        String bio,
        Integer yearsExperience,
        String gender,
        String licenseUrl) {

    /**
     * Parses one snapshot from a JSON object (an event payload or a reconciliation list item).
     * A field that is absent or JSON {@code null} maps to {@code null}. Returns {@code null} when
     * the mandatory {@code profileId} is missing or unparseable, so callers can treat it as
     * malformed.
     */
    public static TherapistProfileSnapshot fromJson(JsonNode node) {
        if (node == null) {
            return null;
        }
        UUID profileId = parseUuidOrNull(text(node, "profileId"));
        if (profileId == null) {
            return null;
        }
        return new TherapistProfileSnapshot(
                profileId,
                text(node, "fullName"),
                text(node, "specialization"),
                text(node, "bio"),
                integer(node, "yearsExperience"),
                text(node, "gender"),
                text(node, "licenseUrl"));
    }

    private static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private static Integer integer(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asInt() : null;
    }

    private static UUID parseUuidOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
