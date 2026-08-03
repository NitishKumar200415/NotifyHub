package com.notifyhub.notification.controller;

import com.notifyhub.auth.AppUserDetails;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.net.URI;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {


    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> send(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody SendNotificationRequest request) {

        NotificationResponse response = notificationService.send(
                userDetails.getAppUser(),
                request
        );

        return ResponseEntity
                .created(URI.create("/api/notifications/" + response.getId()))
                .body(response);
    }
}