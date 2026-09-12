package com.notifyhub.notification.controller;

import com.notifyhub.auth.AppUserDetails;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.NotificationStatsResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            summary = "Get notification history",
            description = "Returns the authenticated user's notification history with pagination and optional status and channel filters."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification history retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
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
    @Operation(
            summary = "Send a notification",
            description = "Creates and sends a notification through the requested delivery channel."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Notification created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid notification data"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
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
    @Operation(
            summary = "Get notification statistics",
            description = "Returns notification delivery statistics for the currently authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification statistics retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
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
    @Operation(
            summary = "Retry a notification",
            description = "Manually retries a notification that previously failed or was dead-lettered."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification retry initiated successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notification not found"
            )
    })
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
    @Operation(
            summary = "Get notification by ID",
            description = "Returns details of a specific notification belonging to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notification not found"
            )
    })
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

