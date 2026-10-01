package com.example.resilience.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
    public static final String EXCHANGE = "resilience.events";
    public static final String QUEUE = "allocation.notification";

    @Bean TopicExchange eventExchange() { return new TopicExchange(EXCHANGE, true, false); }
    @Bean Queue notificationQueue() { return QueueBuilder.durable(QUEUE).build(); }
    @Bean Binding notificationBinding(Queue notificationQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(notificationQueue).to(eventExchange).with("allocation.#");
    }
}
