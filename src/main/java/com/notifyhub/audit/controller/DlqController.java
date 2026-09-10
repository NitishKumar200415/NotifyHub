package com.notifyhub.audit.controller;

import com.notifyhub.audit.dto.DlqNotificationResponse;
import com.notifyhub.audit.service.DlqService;
import com.notifyhub.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dlq")
@RequiredArgsConstructor
public class DlqController {

    private final DlqService dlqService;

    /*
     * Get all dead-lettered notifications.
     */
    @GetMapping
    public List<DlqNotificationResponse> getDeadLetteredNotifications() {

        return dlqService.getDeadLetteredNotifications();
    }

    /*
     * Reprocess a dead-lettered notification.
     */
    @PostMapping("/{id}/reprocess")
    public NotificationResponse reprocess(
            @PathVariable Long id) {

        return dlqService.reprocess(id);
    }
}