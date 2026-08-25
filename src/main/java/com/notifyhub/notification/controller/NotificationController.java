package com.notifyhub.notification.controller;

import com.notifyhub.auth.AppUserDetails;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.NotificationStatsResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    /*
     * Get notification history.
     *
     * Supports:
     * - Pagination
     * - Status filtering
     * - Channel filtering
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getAll(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationChannel channel) {

        return ResponseEntity.ok(
                notificationService.getAll(
                        userDetails.getAppUser(),
                        page,
                        size,
                        status,
                        channel
                )
        );
    }

    /*
     * Create and send a new notification.
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> send(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            ) String idempotencyKey,
            @Valid @RequestBody SendNotificationRequest request) {

        request.setIdempotencyKey(idempotencyKey);

        NotificationResponse response = notificationService.send(
                userDetails.getAppUser(),
                request
        );

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/notifications/" + response.getId()
                        )
                )
                .body(response);
    }

    /*
     * Get notification statistics for the currently logged-in user.
     *
     * Example:
     * GET /api/notifications/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsResponse> getStats(
            @AuthenticationPrincipal AppUserDetails userDetails) {

        return ResponseEntity.ok(
                notificationService.getStats(
                        userDetails.getAppUser()
                )
        );
    }

    /*
     * Manually retry a notification.
     *
     * Only notifications with FAILED or DEAD_LETTERED
     * status should be allowed to retry.
     *
     * Example:
     * POST /api/notifications/123/retry
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<NotificationResponse> retry(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        return ResponseEntity.ok(
                notificationService.retry(
                        userDetails.getAppUser(),
                        id
                )
        );
    }

    /*
     * Get a specific notification.
     *
     * This endpoint is placed after fixed paths such as
     * "/stats" to avoid Spring interpreting "stats" as an ID.
     */
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