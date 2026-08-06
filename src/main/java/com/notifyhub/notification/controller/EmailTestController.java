package com.notifyhub.notification.controller;

import com.notifyhub.notification.service.EmailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailSenderService emailSenderService;

    @GetMapping("/api/test-email")
    public String sendTestEmail() {

        emailSenderService.sendEmail(
                "nitishkumar636232@gmail.com",
                "NotifyHub Test Email",
                "🎉 Congratulations! Your first email from NotifyHub is working."
        );

        return "Email request sent.";
    }
}