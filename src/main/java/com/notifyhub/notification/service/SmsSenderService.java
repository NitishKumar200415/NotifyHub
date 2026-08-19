package com.notifyhub.notification.service;

public interface SmsSenderService {

    boolean sendSms(String to, String body);
}