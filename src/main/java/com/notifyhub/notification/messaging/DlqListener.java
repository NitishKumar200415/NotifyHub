package com.notifyhub.notification.messaging;

import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DlqListener {

    private final NotificationRepository notificationRepository;

    @RabbitListener(queues = {
            "notifyhub.email.dlq",
            "notifyhub.sms.dlq",
            "notifyhub.push.dlq"
    })
    public void consume(NotificationEvent event) {

        Notification notification = notificationRepository
                .findById(event.notificationId())
                .orElseThrow();

        notification.setStatus(
                NotificationStatus.DEAD_LETTERED
        );

        notificationRepository.save(notification);

        log.error(
                "Notification {} moved to dead-letter queue after retry exhaustion",
                notification.getId()
        );
    }
}