package com.booking.therapist_api.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the therapist-profile read-model stream consumed from auth-service — the first
 * inbound consumer in therapist-api.
 *
 * <p>Declares a <b>dedicated, durable</b> queue bound to auth-service's {@code auth.events} topic
 * exchange on routing key {@code therapist.profile.updated}, plus a dead-letter exchange/queue for
 * poison messages. A dedicated queue name (rather than a shared one) guarantees therapist-api
 * receives <i>every</i> profile event instead of competing with other consumers, and lets us attach
 * DLQ args cleanly.
 *
 * <p>The listener container factory uses <b>manual</b> acknowledgement with a single consumer so the
 * consumer controls ack/dead-letter, same-key events stay serialized (keeping the watermark guard
 * correct under replays), and a failure never falls into an infinite nack-requeue loop.
 *
 * <p>The topic exchange is owned by auth-service; we redeclare it idempotently (matching
 * type/durability) so therapist-api can boot and bind even if it starts first. The exchange name is
 * configurable via {@code mhsa.auth.events.exchange} and must match auth-service's value.
 */
@Configuration
public class AuthEventsMessagingConfig {

    public static final String THERAPIST_PROFILE_UPDATED_ROUTING_KEY = "therapist.profile.updated";

    public static final String THERAPIST_PROFILE_QUEUE = "therapist-api.auth.therapist.profile.updated";

    public static final String THERAPIST_PROFILE_DLX = "therapist-api.auth.therapist.profile.dlx";
    public static final String THERAPIST_PROFILE_DLQ = "therapist-api.auth.therapist.profile.dlq";

    public static final String MANUAL_ACK_FACTORY = "authProfileManualAckListenerContainerFactory";

    /** Name of auth-service's topic exchange. Must match {@code mhsa.auth.events.exchange} there. */
    @Value("${mhsa.auth.events.exchange:auth.events}")
    private String authEventsExchange;

    @Bean
    public TopicExchange authEventsExchange() {
        return new TopicExchange(authEventsExchange, true, false);
    }

    @Bean
    public Queue therapistProfileUpdatedQueue() {
        return QueueBuilder.durable(THERAPIST_PROFILE_QUEUE)
                .withArgument("x-dead-letter-exchange", THERAPIST_PROFILE_DLX)
                .withArgument("x-dead-letter-routing-key", THERAPIST_PROFILE_DLQ)
                .build();
    }

    @Bean
    public Binding therapistProfileUpdatedBinding() {
        return BindingBuilder.bind(therapistProfileUpdatedQueue())
                .to(authEventsExchange())
                .with(THERAPIST_PROFILE_UPDATED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange therapistProfileDeadLetterExchange() {
        return new DirectExchange(THERAPIST_PROFILE_DLX, true, false);
    }

    @Bean
    public Queue therapistProfileDeadLetterQueue() {
        return QueueBuilder.durable(THERAPIST_PROFILE_DLQ).build();
    }

    @Bean
    public Binding therapistProfileDeadLetterBinding() {
        return BindingBuilder.bind(therapistProfileDeadLetterQueue())
                .to(therapistProfileDeadLetterExchange())
                .with(THERAPIST_PROFILE_DLQ);
    }

    @Bean(MANUAL_ACK_FACTORY)
    public SimpleRabbitListenerContainerFactory authProfileManualAckListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
