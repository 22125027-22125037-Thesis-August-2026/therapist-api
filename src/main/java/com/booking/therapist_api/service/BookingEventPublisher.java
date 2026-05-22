package com.booking.therapist_api.service;

import com.booking.therapist_api.config.RabbitMQConfig;
import com.booking.therapist_api.event.AppointmentBookedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class BookingEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public BookingEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes the event to RabbitMQ only after the booking transaction has
     * successfully committed. If the commit rolls back, this listener never
     * fires, so no notification is sent for a booking that did not persist.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentBooked(AppointmentBookedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOKING_EXCHANGE,
                    RabbitMQConfig.APPOINTMENT_BOOKED_ROUTING_KEY,
                    event
            );

            LOGGER.info(
                    "Published appointment booked event messageId={} appointmentId={} profileId={}",
                    event.messageId(),
                    event.appointmentId(),
                    event.profileId()
            );
        } catch (AmqpException ex) {
            // The booking is already committed at this point. A broker outage must
            // not surface as a failed booking, so we log and let it through.
            LOGGER.error(
                    "Failed to publish appointment booked event messageId={} appointmentId={}",
                    event.messageId(),
                    event.appointmentId(),
                    ex
            );
        }
    }
}
