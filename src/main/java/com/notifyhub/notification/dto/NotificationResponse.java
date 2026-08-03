package com.notifyhub.notification.dto;

import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.notification.entity.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;

    private String recipientAddress;

    private NotificationChannel channel;

    private NotificationStatus status;

    private Instant createdAt;

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipientAddress(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getCreatedAt()
        );
    }
}