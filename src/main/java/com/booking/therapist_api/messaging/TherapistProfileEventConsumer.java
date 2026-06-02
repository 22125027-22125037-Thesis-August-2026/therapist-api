package com.booking.therapist_api.messaging;

import com.booking.therapist_api.config.AuthEventsMessagingConfig;
import com.booking.therapist_api.service.TherapistProfileReplicaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Consumes auth-service {@code therapist.profile.updated} events and mirrors the auth-owned profile
 * fields into the local {@code therapists} read replica.
 *
 * <p>Manual ack with a single consumer (see {@link AuthEventsMessagingConfig}). Failure handling
 * avoids infinite nack loops:
 * <ul>
 *   <li>Malformed / unparseable / missing-id / missing-occurredAt messages are dead-lettered
 *       immediately.</li>
 *   <li>Transient processing errors are also dead-lettered (requeue=false) and left for the nightly
 *       reconciliation job to heal, rather than requeued forever.</li>
 * </ul>
 */
@Component
public class TherapistProfileEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TherapistProfileEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final TherapistProfileReplicaService replicaService;

    public TherapistProfileEventConsumer(ObjectMapper objectMapper,
                                         TherapistProfileReplicaService replicaService) {
        this.objectMapper = objectMapper;
        this.replicaService = replicaService;
    }

    @RabbitListener(
            queues = AuthEventsMessagingConfig.THERAPIST_PROFILE_QUEUE,
            containerFactory = AuthEventsMessagingConfig.MANUAL_ACK_FACTORY)
    public void onTherapistProfileUpdated(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        TherapistProfileSnapshot snapshot;
        Instant occurredAt;
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(body);
            snapshot = TherapistProfileSnapshot.fromJson(node);
            occurredAt = parseInstant(node, "occurredAt");
            if (snapshot == null) {
                throw new IllegalArgumentException("missing/invalid profileId: " + body);
            }
            if (occurredAt == null) {
                throw new IllegalArgumentException("missing/invalid occurredAt: " + body);
            }
        } catch (Exception parseError) {
            LOGGER.error("Malformed therapist profile event, dead-lettering: {}", parseError.getMessage());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        try {
            replicaService.applyEvent(snapshot, occurredAt);
            channel.basicAck(deliveryTag, false);
        } catch (Exception processingError) {
            // Transient failure (e.g. DB unavailable): dead-letter instead of requeue-looping.
            // The nightly reconciliation job converges the replica back to the snapshot.
            LOGGER.error("Failed to apply therapist profile event (therapistId={}), dead-lettering",
                    snapshot.profileId(), processingError);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private static Instant parseInstant(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        try {
            return Instant.parse(node.get(field).asText());
        } catch (Exception e) {
            return null;
        }
    }
}
