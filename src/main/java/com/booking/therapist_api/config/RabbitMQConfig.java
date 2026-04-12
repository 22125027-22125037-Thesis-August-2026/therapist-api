package com.booking.therapist_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String NOTIFICATION_BOOKING_QUEUE = "notification.booking.queue";
    public static final String APPOINTMENT_BOOKED_ROUTING_KEY = "appointment.booked";
    public static final String PROFILE_DEMOGRAPHICS_UPDATED_ROUTING_KEY = "profile.demographics.updated";
    public static final String TRACKING_MOOD_LOGGED_ROUTING_KEY = "tracking.mood.logged";
    public static final String AI_CRISIS_ALERTED_ROUTING_KEY = "ai.crisis.alerted";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationBookingQueue() {
        return new Queue(NOTIFICATION_BOOKING_QUEUE, true, false, false);
    }

    @Bean
    public Binding appointmentBookedBinding(Queue notificationBookingQueue, TopicExchange bookingExchange) {
        return BindingBuilder
                .bind(notificationBookingQueue)
                .to(bookingExchange)
                .with(APPOINTMENT_BOOKED_ROUTING_KEY);
    }

    @Bean
    public ObjectMapper rabbitObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper rabbitObjectMapper) {
        return new Jackson2JsonMessageConverter(rabbitObjectMapper);
    }
}
