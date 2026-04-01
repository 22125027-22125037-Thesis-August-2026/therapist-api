package com.booking.therapist_api.service;

import com.booking.therapist_api.config.RabbitMQConfig;
import com.booking.therapist_api.event.AppointmentBookedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class BookingEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public BookingEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishAppointmentBooked(UUID appointmentId) {
        AppointmentBookedEvent event = new AppointmentBookedEvent(appointmentId, Instant.now());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.BOOKING_EXCHANGE,
                RabbitMQConfig.APPOINTMENT_BOOKED_ROUTING_KEY,
                event
        );

        LOGGER.info("Published appointment booked event for appointmentId={}", appointmentId);
    }
}
