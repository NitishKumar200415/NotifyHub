package com.notifyhub.notification.messaging;

import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.notification.service.EmailSenderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.notifyhub.audit.entity.NotificationAttempt;
import com.notifyhub.audit.repository.NotificationAttemptRepository;

@Component
@RequiredArgsConstructor
public class EmailNotificationConsumer {
    private final NotificationRepository notificationRepository;
    private final EmailSenderService emailSenderService;
    private final NotificationAttemptRepository notificationAttemptRepository;

    @RabbitListener(queues = "notifyhub.email.queue")
    public void consume(NotificationEvent event) {

        MDC.put("correlationId", event.correlationId());

        try {

            Notification notification = notificationRepository
                    .findById(event.notificationId())
                    .orElseThrow();

            boolean emailSent = emailSenderService.sendEmail(
                    notification.getRecipientAddress(),
                    "NotifyHub Notification",
                    notification.getPayload()
            );

            if (emailSent) {

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

            int retryCount = notification.getRetryCount() + 1;

            notificationAttemptRepository.save(
                    NotificationAttempt.builder()
                            .notificationId(notification.getId())
                            .attemptNumber(retryCount)
                            .status(NotificationAttempt.AttemptStatus.FAILURE)
                            .errorMessage("Email sending failed")
                            .build()
            );

            notification.setRetryCount(retryCount);
            notificationRepository.save(notification);

            if (retryCount >= 4) {
                throw new RuntimeException(
                        "Email sending failed after max retries"
                );
            }

            throw new RuntimeException("Email sending failed");

        } finally {
            MDC.remove("correlationId");
        }
    }
}