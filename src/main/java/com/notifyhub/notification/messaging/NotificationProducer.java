package com.notifyhub.notification.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public NotificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(NotificationEvent event) {

        String routingKey = event.channel().toLowerCase();

        rabbitTemplate.convertAndSend(
                "notifyhub.notification.exchange",
                routingKey,
                event
        );
    }
}