package com.notifyhub.notification.messaging;

import java.io.Serializable;

public record NotificationEvent(
        Long notificationId,
        String recipientAddress,
        String channel,
        String correlationId
) implements Serializable {
}