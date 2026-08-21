package com.notifyhub.notification.messaging;

import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.notification.service.SmsSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsNotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final SmsSenderService smsSenderService;

    @RabbitListener(queues = "notifyhub.sms.queue")
    public void consume(NotificationEvent event) {

        Notification notification = notificationRepository
                .findById(event.notificationId())
                .orElseThrow();

        boolean smsSent = smsSenderService.sendSms(
                notification.getRecipientAddress(),
                notification.getPayload()
        );

        if (smsSent) {
            notification.setStatus(NotificationStatus.SENT);
            notificationRepository.save(notification);
            return;
        }

        int retryCount = notification.getRetryCount() + 1;

        notification.setRetryCount(retryCount);
        notificationRepository.save(notification);

        if (retryCount >= 4) {
            throw new RuntimeException(
                    "SMS sending failed after max retries"
            );
        }

        throw new RuntimeException("SMS sending failed");
    }
}