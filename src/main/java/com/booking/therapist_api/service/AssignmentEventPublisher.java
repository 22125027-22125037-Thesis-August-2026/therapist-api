package com.booking.therapist_api.service;

import com.booking.therapist_api.config.RabbitMQConfig;
import com.booking.therapist_api.event.AssignmentChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class AssignmentEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public AssignmentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes the assignment change to RabbitMQ only after the surrounding
     * transaction has committed. If the commit rolls back, this listener never
     * fires, so no event is emitted for a change that did not persist.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAssignmentChanged(AssignmentChangedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOKING_EXCHANGE,
                    RabbitMQConfig.ASSIGNMENT_CHANGED_ROUTING_KEY,
                    event
            );

            LOGGER.info(
                    "Published assignment changed event eventId={} therapistProfileId={} patientProfileId={} status={}",
                    event.eventId(),
                    event.therapistProfileId(),
                    event.patientProfileId(),
                    event.status()
            );
        } catch (AmqpException ex) {
            // The assignment is already committed at this point. A broker outage must
            // not surface as a failed assignment, so we log and let it through.
            LOGGER.error(
                    "Failed to publish assignment changed event eventId={} therapistProfileId={} patientProfileId={}",
                    event.eventId(),
                    event.therapistProfileId(),
                    event.patientProfileId(),
                    ex
            );
        }
    }
}
