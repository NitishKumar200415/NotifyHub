package com.notifyhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange("notifyhub.notification.exchange");
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("notifyhub.notification.dlx");
    }

    @Bean
    public Queue emailQueue() {

        Map<String, Object> arguments = new HashMap<>();

        arguments.put(
                "x-dead-letter-exchange",
                "notifyhub.notification.dlx"
        );

        arguments.put(
                "x-dead-letter-routing-key",
                "email.dlq"
        );

        return new Queue(
                "notifyhub.email.queue",
                true,
                false,
                false,
                arguments
        );
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue("notifyhub.email.dlq");
    }

    @Bean
    public Binding emailBinding(
            Queue emailQueue,
            DirectExchange notificationExchange
    ) {
        return BindingBuilder
                .bind(emailQueue)
                .to(notificationExchange)
                .with("email");
    }

    @Bean
    public Binding deadLetterBinding(
            Queue deadLetterQueue,
            DirectExchange deadLetterExchange
    ) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("email.dlq");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(
            ObjectMapper objectMapper
    ) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter
    ) {
        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(messageConverter);

        return rabbitTemplate;
    }
}