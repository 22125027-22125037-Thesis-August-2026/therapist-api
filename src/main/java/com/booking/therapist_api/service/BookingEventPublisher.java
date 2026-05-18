package com.booking.therapist_api.service;

import com.booking.therapist_api.config.RabbitMQConfig;
import com.booking.therapist_api.entity.Appointment;
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

    public void publishAppointmentBooked(Appointment appointment, String userEmail, String userName) {
        UUID messageId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        String safeUserEmail = userEmail == null ? "" : userEmail;
        String safeUserName = userName == null ? "" : userName;

        AppointmentBookedEvent event = new AppointmentBookedEvent(
                messageId,
                occurredAt,
                appointment.getId(),
                appointment.getProfileId(),
                safeUserEmail,
                safeUserName,
                appointment.getTherapist().getFullName(),
                appointment.getStartDatetime()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.BOOKING_EXCHANGE,
                RabbitMQConfig.APPOINTMENT_BOOKED_ROUTING_KEY,
                event
        );

        LOGGER.info(
                "Published appointment booked event messageId={} appointmentId={} profileId={}",
                messageId,
                appointment.getId(),
                appointment.getProfileId()
        );
    }
}
