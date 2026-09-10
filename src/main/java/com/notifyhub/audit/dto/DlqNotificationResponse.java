package com.notifyhub.audit.dto;

import com.notifyhub.audit.entity.NotificationAttempt;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.notification.entity.NotificationStatus;

import java.time.Instant;
import java.util.List;

public record DlqNotificationResponse(
        Long notificationId,
        String recipientAddress,
        NotificationChannel channel,
        NotificationStatus status,
        Integer retryCount,
        Instant createdAt,
        Instant updatedAt,
        List<AttemptResponse> attempts
) {

    public record AttemptResponse(
            Integer attemptNumber,
            NotificationAttempt.AttemptStatus status,
            String errorMessage,
            Instant attemptedAt
    ) {
    }
}
