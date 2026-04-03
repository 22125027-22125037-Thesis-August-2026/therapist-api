package com.booking.therapist_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RabbitMQPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQPublisher.class);

    public void publishAppointmentBookedEvent(UUID appointmentId) {
        // Placeholder publisher for asynchronous downstream processing.
        LOGGER.info("Published appointment booked event for appointmentId={}", appointmentId);
    }
}
