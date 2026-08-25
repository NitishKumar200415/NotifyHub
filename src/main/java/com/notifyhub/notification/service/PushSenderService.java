package com.notifyhub.notification.service;

public interface PushSenderService {

    boolean sendPush(
            String deviceToken,
            String message
    );
}