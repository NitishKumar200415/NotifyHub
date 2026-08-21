package com.notifyhub.notification.controller;

import com.notifyhub.auth.AppUserDetails;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAll(
            @AuthenticationPrincipal AppUserDetails userDetails) {

        return ResponseEntity.ok(
                notificationService.getAll(
                        userDetails.getAppUser()
                )
        );
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> send(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey,
            @Valid @RequestBody SendNotificationRequest request) {

        request.setIdempotencyKey(idempotencyKey);

        NotificationResponse response = notificationService.send(
                userDetails.getAppUser(),
                request
        );

        return ResponseEntity
                .created(URI.create("/api/notifications/" + response.getId()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getById(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        return ResponseEntity.ok(
                notificationService.getById(
                        userDetails.getAppUser(),
                        id
                )
        );
    }
}