package com.notifyhub.notification.messaging;

import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.notification.service.EmailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final EmailSenderService emailSenderService;

    @RabbitListener(queues = "notifyhub.email.queue")
    public void consume(NotificationEvent event) {

        Notification notification = notificationRepository
                .findById(event.notificationId())
                .orElseThrow();

        boolean emailSent = emailSenderService.sendEmail(
                notification.getRecipientAddress(),
                "NotifyHub Notification",
                notification.getPayload()
        );

        if (emailSent) {

            notification.setStatus(NotificationStatus.SENT);
            notificationRepository.save(notification);

            return;
        }

        int retryCount = notification.getRetryCount() + 1;
        notification.setRetryCount(retryCount);

        if (retryCount >= 4) {

            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);

            return;
        }

        notificationRepository.save(notification);

        throw new RuntimeException("Email sending failed");
    }
}