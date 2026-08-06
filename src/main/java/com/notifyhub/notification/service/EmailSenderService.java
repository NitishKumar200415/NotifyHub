package com.notifyhub.notification.service;

public interface EmailSenderService {

    boolean sendEmail(String to,
                      String subject,
                      String body);

}