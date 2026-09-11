package com.notifyhub.notification.messaging;

import com.notifyhub.audit.entity.NotificationAttempt;
import com.notifyhub.audit.repository.NotificationAttemptRepository;
import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.notification.service.PushSenderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PushNotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final PushSenderService pushSenderService;
    private final NotificationAttemptRepository notificationAttemptRepository;

    @RabbitListener(queues = "notifyhub.push.queue")
    public void consume(NotificationEvent event) {

        MDC.put("correlationId", event.correlationId());

        try {

            Notification notification = notificationRepository
                    .findById(event.notificationId())
                    .orElseThrow();

            boolean pushSent = pushSenderService.sendPush(
                    notification.getRecipientAddress(),
                    notification.getPayload()
            );

            if (pushSent) {

                notificationAttemptRepository.save(
                        NotificationAttempt.builder()
                                .notificationId(notification.getId())
                                .attemptNumber(notification.getRetryCount() + 1)
                                .status(NotificationAttempt.AttemptStatus.SUCCESS)
                                .build()
                );

                notification.setStatus(NotificationStatus.SENT);

                notificationRepository.save(notification);

                return;
            }

            int retryCount =
                    notification.getRetryCount() + 1;

            notificationAttemptRepository.save(
                    NotificationAttempt.builder()
                            .notificationId(notification.getId())
                            .attemptNumber(retryCount)
                            .status(NotificationAttempt.AttemptStatus.FAILURE)
                            .errorMessage("PUSH notification failed")
                            .build()
            );

            notification.setRetryCount(retryCount);

            notificationRepository.save(notification);

            if (retryCount >= 4) {

                throw new RuntimeException(
                        "PUSH notification failed after max retries"
                );
            }

            throw new RuntimeException(
                    "PUSH notification failed"
            );

        } finally {
            MDC.remove("correlationId");
        }
    }
}