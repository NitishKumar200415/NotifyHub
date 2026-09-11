package com.notifyhub.notification.messaging;

import com.notifyhub.audit.entity.NotificationAttempt;
import com.notifyhub.audit.repository.NotificationAttemptRepository;
import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.notification.service.SmsSenderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsNotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final SmsSenderService smsSenderService;
    private final NotificationAttemptRepository notificationAttemptRepository;

    @RabbitListener(queues = "notifyhub.sms.queue")
    public void consume(NotificationEvent event) {

        MDC.put("correlationId", event.correlationId());

        try {

            Notification notification = notificationRepository
                    .findById(event.notificationId())
                    .orElseThrow();

            boolean smsSent = smsSenderService.sendSms(
                    notification.getRecipientAddress(),
                    notification.getPayload()
            );

            if (smsSent) {

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
                            .errorMessage("SMS sending failed")
                            .build()
            );

            notification.setRetryCount(retryCount);
            notificationRepository.save(notification);

            if (retryCount >= 4) {
                throw new RuntimeException(
                        "SMS sending failed after max retries"
                );
            }

            throw new RuntimeException("SMS sending failed");

        } finally {
            MDC.remove("correlationId");
        }
    }
}