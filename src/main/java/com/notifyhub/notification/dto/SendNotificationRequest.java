package com.notifyhub.notification.dto;

import com.notifyhub.notification.entity.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    @NotBlank(message = "Recipient address is required")
    private String recipientAddress;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    private String templateCode;

    private String payload;

    private String idempotencyKey;
}