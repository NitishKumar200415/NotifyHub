package com.notifyhub.audit;

import com.notifyhub.audit.entity.NotificationAttempt;
import com.notifyhub.audit.repository.NotificationAttemptRepository;
import com.notifyhub.audit.service.DlqService;
import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.messaging.NotificationProducer;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.notification.dto.NotificationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
class DlqIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationAttemptRepository notificationAttemptRepository;

    @Autowired
    private DlqService dlqService;

    @MockBean
    private NotificationProducer notificationProducer;

    @Test
    void shouldNotReprocessNonDeadLetteredNotification() {

        Notification notification = new Notification();

        notification.setRecipientAddress(
                "normal@example.com"
        );

        notification.setChannel(
                NotificationChannel.EMAIL
        );

        notification.setStatus(
                NotificationStatus.QUEUED
        );

        notification.setRetryCount(0);

        Notification savedNotification =
                notificationRepository.save(notification);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> dlqService.reprocess(
                                savedNotification.getId()
                        )
                )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Only dead-lettered notifications can be reprocessed"
                );
    }

    @Test
    void shouldStoreAndRetrieveNotificationAttempts() {

        Notification notification = new Notification();

        notification.setRecipientAddress("test@example.com");
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.DEAD_LETTERED);
        notification.setRetryCount(4);

        Notification savedNotification =
                notificationRepository.save(notification);

        NotificationAttempt failureAttempt =
                NotificationAttempt.builder()
                        .notificationId(savedNotification.getId())
                        .attemptNumber(1)
                        .status(
                                NotificationAttempt.AttemptStatus.FAILURE
                        )
                        .errorMessage("Email sending failed")
                        .build();

        NotificationAttempt successAttempt =
                NotificationAttempt.builder()
                        .notificationId(savedNotification.getId())
                        .attemptNumber(2)
                        .status(
                                NotificationAttempt.AttemptStatus.SUCCESS
                        )
                        .build();

        notificationAttemptRepository.save(failureAttempt);
        notificationAttemptRepository.save(successAttempt);

        List<NotificationAttempt> attempts =
                notificationAttemptRepository
                        .findByNotificationIdOrderByAttemptNumberAsc(
                                savedNotification.getId()
                        );

        assertThat(attempts).hasSize(2);

        assertThat(attempts.get(0).getAttemptNumber())
                .isEqualTo(1);

        assertThat(attempts.get(0).getStatus())
                .isEqualTo(
                        NotificationAttempt.AttemptStatus.FAILURE
                );

        assertThat(attempts.get(1).getAttemptNumber())
                .isEqualTo(2);

        assertThat(attempts.get(1).getStatus())
                .isEqualTo(
                        NotificationAttempt.AttemptStatus.SUCCESS
                );
    }

    @Test
    void shouldReprocessDeadLetteredNotification() {

        Notification notification = new Notification();

        notification.setRecipientAddress(
                "reprocess@example.com"
        );

        notification.setChannel(
                NotificationChannel.EMAIL
        );

        notification.setStatus(
                NotificationStatus.DEAD_LETTERED
        );

        notification.setRetryCount(4);

        Notification savedNotification =
                notificationRepository.save(notification);

        NotificationResponse response =
                dlqService.reprocess(
                        savedNotification.getId()
                );

        Notification updatedNotification =
                notificationRepository
                        .findById(savedNotification.getId())
                        .orElseThrow();

        assertThat(updatedNotification.getStatus())
                .isEqualTo(NotificationStatus.QUEUED);

        assertThat(updatedNotification.getRetryCount())
                .isEqualTo(0);

        assertThat(response.getId())
                .isEqualTo(savedNotification.getId());

        assertThat(response.getStatus())
                .isEqualTo(NotificationStatus.QUEUED);

        verify(notificationProducer)
                .publish(
                        org.mockito.ArgumentMatchers.any()
                );
    }
}