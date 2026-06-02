package com.booking.therapist_api.service;

import com.booking.therapist_api.messaging.TherapistProfileSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Nightly safety net that heals any {@code therapist.profile.updated} events therapist-api missed
 * (broker outage, dead-lettered messages, downtime). Pages auth-service's
 * {@code GET /internal/therapist-profiles} snapshot and upserts the auth-owned columns for every
 * therapist that exists locally.
 *
 * <p><b>Fail-safe:</b> the whole snapshot is fetched and validated <i>before</i> any write. If a
 * page fetch errors, returns malformed JSON, or the snapshot is empty, the run aborts without
 * touching the database — a transient auth-service problem must never corrupt the replica.
 */
@Service
public class TherapistProfileReconciliationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TherapistProfileReconciliationService.class);

    private static final String SNAPSHOT_PATH = "/internal/therapist-profiles";

    private final TherapistProfileReplicaService replicaService;
    private final RestClient authInternalClient;
    private final int pageSize;

    public TherapistProfileReconciliationService(
            TherapistProfileReplicaService replicaService,
            @Value("${mhsa.auth.internal.base-url:http://auth-service:8081}") String authBaseUrl,
            @Value("${mhsa.auth.internal.reconcile.page-size:100}") int pageSize) {
        this.replicaService = replicaService;
        // RestClient.create wires the default HTTP client + Jackson converters without
        // depending on an auto-configured RestClient.Builder bean.
        this.authInternalClient = RestClient.create(authBaseUrl);
        this.pageSize = pageSize;
    }

    /**
     * Runs nightly at 23:45 Asia/Ho_Chi_Minh by default. Cron and time zone are configurable.
     */
    @Scheduled(
            cron = "${mhsa.auth.internal.reconcile.cron:0 45 23 * * *}",
            zone = "${mhsa.auth.internal.reconcile.zone:Asia/Ho_Chi_Minh}")
    public void reconcile() {
        List<TherapistProfileSnapshot> snapshots = fetchAllSnapshots();
        if (snapshots == null) {
            // fetchAllSnapshots already logged the reason; abort without writing.
            return;
        }
        if (snapshots.isEmpty()) {
            LOGGER.warn("Therapist profile reconciliation aborted: auth-service returned an empty snapshot");
            return;
        }

        int updated = replicaService.reconcile(snapshots);
        LOGGER.info("Therapist profile reconciliation complete: {} fetched, {} updated",
                snapshots.size(), updated);
    }

    /**
     * Fetches and parses every page of the auth-service snapshot. Returns {@code null} on any fetch
     * or parse failure (caller must abort); returns an empty list only if auth-service genuinely
     * reports zero profiles.
     */
    private List<TherapistProfileSnapshot> fetchAllSnapshots() {
        List<TherapistProfileSnapshot> all = new ArrayList<>();
        int page = 0;
        while (true) {
            JsonNode body;
            try {
                final int currentPage = page;
                body = authInternalClient.get()
                        .uri(uriBuilder -> uriBuilder.path(SNAPSHOT_PATH)
                                .queryParam("page", currentPage)
                                .queryParam("size", pageSize)
                                .build())
                        .retrieve()
                        .body(JsonNode.class);
            } catch (Exception fetchError) {
                LOGGER.error("Therapist profile reconciliation aborted: failed to fetch page {} from auth-service",
                        page, fetchError);
                return null;
            }

            JsonNode content = body == null ? null : body.get("content");
            if (content == null || !content.isArray()) {
                LOGGER.error("Therapist profile reconciliation aborted: malformed snapshot on page {} ({})",
                        page, body);
                return null;
            }

            for (JsonNode item : content) {
                TherapistProfileSnapshot snapshot = TherapistProfileSnapshot.fromJson(item);
                if (snapshot == null) {
                    LOGGER.error("Therapist profile reconciliation aborted: malformed profile entry on page {}", page);
                    return null;
                }
                all.add(snapshot);
            }

            boolean last = body.path("last").asBoolean(true);
            if (last || content.isEmpty()) {
                break;
            }
            page++;
        }
        return all;
    }
}
