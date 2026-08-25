package com.notifyhub.notification.messaging;

import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.notification.service.PushSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PushNotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final PushSenderService pushSenderService;

    @RabbitListener(queues = "notifyhub.push.queue")
    public void consume(NotificationEvent event) {

        Notification notification = notificationRepository
                .findById(event.notificationId())
                .orElseThrow();

        boolean pushSent = pushSenderService.sendPush(
                notification.getRecipientAddress(),
                notification.getPayload()
        );

        if (pushSent) {

            notification.setStatus(NotificationStatus.SENT);

            notificationRepository.save(notification);

            return;
        }

        int retryCount =
                notification.getRetryCount() + 1;

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
    }
}