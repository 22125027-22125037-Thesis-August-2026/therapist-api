package com.booking.therapist_api.config;

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

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    @Bean
    public Queue notificationBookingQueue() {
        return new Queue(NOTIFICATION_BOOKING_QUEUE);
    }

    @Bean
    public Binding appointmentBookedBinding(Queue notificationBookingQueue, TopicExchange bookingExchange) {
        return BindingBuilder
                .bind(notificationBookingQueue)
                .to(bookingExchange)
                .with(APPOINTMENT_BOOKED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
