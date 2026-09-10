package com.notifyhub.audit.service;

import com.notifyhub.audit.dto.DlqNotificationResponse;
import com.notifyhub.audit.entity.NotificationAttempt;
import com.notifyhub.audit.repository.NotificationAttemptRepository;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.messaging.NotificationEvent;
import com.notifyhub.notification.messaging.NotificationProducer;
import com.notifyhub.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DlqService {

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository notificationAttemptRepository;
    private final NotificationProducer notificationProducer;

    /*
     * Get all notifications that are currently
     * DEAD_LETTERED.
     */
    public List<DlqNotificationResponse> getDeadLetteredNotifications() {

        return notificationRepository
                .findAll()
                .stream()
                .filter(notification ->
                        notification.getStatus()
                                == NotificationStatus.DEAD_LETTERED
                )
                .map(this::toResponse)
                .toList();
    }

    /*
     * Reprocess a dead-lettered notification.
     */
    public NotificationResponse reprocess(Long notificationId) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Notification not found"
                        )
                );

        /*
         * Only DEAD_LETTERED notifications
         * can be reprocessed.
         */
        if (notification.getStatus()
                != NotificationStatus.DEAD_LETTERED) {

            throw new IllegalStateException(
                    "Only dead-lettered notifications can be reprocessed"
            );
        }

        /*
         * Put the notification back into
         * the normal processing flow.
         */
        notification.setStatus(NotificationStatus.QUEUED);

        /*
         * Start the retry count from zero
         * for this new processing cycle.
         */
        notification.setRetryCount(0);

        notificationRepository.save(notification);

        /*
         * Publish the notification back to RabbitMQ.
         *
         * NotificationEvent requires:
         * 1. notificationId
         * 2. recipientAddress
         * 3. payload
         */
        notificationProducer.publish(
                new NotificationEvent(
                        notification.getId(),
                        notification.getRecipientAddress(),
                        notification.getPayload()
                )
        );

        return NotificationResponse.from(notification);
    }

    /*
     * Convert Notification entity into
     * DLQ response.
     */
    private DlqNotificationResponse toResponse(
            Notification notification) {

        List<DlqNotificationResponse.AttemptResponse> attempts =
                notificationAttemptRepository
                        .findByNotificationIdOrderByAttemptNumberAsc(
                                notification.getId()
                        )
                        .stream()
                        .map(this::toAttemptResponse)
                        .toList();

        return new DlqNotificationResponse(
                notification.getId(),
                notification.getRecipientAddress(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getRetryCount(),
                notification.getCreatedAt(),
                notification.getUpdatedAt(),
                attempts
        );
    }

    /*
     * Convert NotificationAttempt entity
     * into AttemptResponse DTO.
     */
    private DlqNotificationResponse.AttemptResponse
    toAttemptResponse(NotificationAttempt attempt) {

        return new DlqNotificationResponse.AttemptResponse(
                attempt.getAttemptNumber(),
                attempt.getStatus(),
                attempt.getErrorMessage(),
                attempt.getAttemptedAt()
        );
    }
}