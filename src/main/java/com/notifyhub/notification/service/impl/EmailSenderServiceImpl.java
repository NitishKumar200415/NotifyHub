package com.notifyhub.notification.service.impl;

import com.notifyhub.notification.service.EmailSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderServiceImpl implements EmailSenderService {

    private final JavaMailSender mailSender;

    @Override
    public boolean sendEmail(String to, String subject, String body) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {

            mailSender.send(message);

            log.info("Email sent successfully to {}", to);

            return true;

        } catch (MailException ex) {

            log.error("Failed to send email to {}", to, ex);

            return false;
        }
    }
}