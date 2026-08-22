package com.notifyhub.preference.dto;

import com.notifyhub.notification.entity.NotificationChannel;

public record NotificationPreferenceResponse(
        NotificationChannel channel,
        boolean enabled
) {
}