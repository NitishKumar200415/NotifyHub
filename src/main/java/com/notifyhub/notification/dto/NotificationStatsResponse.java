package com.notifyhub.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationStatsResponse {

    private long total;

    private long queued;

    private long sent;

    private long failed;

    private long deadLettered;

    private long skipped;
}
