package com.notifyhub;

import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.notification.service.NotificationService;
import com.notifyhub.user.AppUser;
import com.notifyhub.user.AppUserRepository;
import com.notifyhub.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationIntegrationTest extends IntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Queue emailQueue;

    @Test
    void shouldSaveAndRetrieveNotification() {

        AppUser user = AppUser.builder()
                .email("integration@test.com")
                .passwordHash("test-password-hash")
                .role(UserRole.CLIENT)
                .build();

        AppUser savedUser = appUserRepository.save(user);

        Notification notification = Notification.builder()
                .recipientUser(savedUser)
                .recipientAddress("integration@test.com")
                .channel(NotificationChannel.EMAIL)
                .payload("Testing PostgreSQL with Testcontainers")
                .status(NotificationStatus.QUEUED)
                .retryCount(0)
                .build();

        Notification savedNotification =
                notificationRepository.save(notification);

        assertThat(savedNotification.getId()).isNotNull();

        Notification retrievedNotification =
                notificationRepository
                        .findById(savedNotification.getId())
                        .orElseThrow();

        assertThat(retrievedNotification.getPayload())
                .isEqualTo("Testing PostgreSQL with Testcontainers");

        assertThat(retrievedNotification.getChannel())
                .isEqualTo(NotificationChannel.EMAIL);

        assertThat(retrievedNotification.getStatus())
                .isEqualTo(NotificationStatus.QUEUED);

        assertThat(retrievedNotification.getRecipientUser().getId())
                .isEqualTo(savedUser.getId());
    }

    @Test
    void shouldCreateAndQueueNotification() {

        AppUser user = AppUser.builder()
                .email("service-integration@test.com")
                .passwordHash("test-password-hash")
                .role(UserRole.CLIENT)
                .build();

        AppUser savedUser = appUserRepository.save(user);

        SendNotificationRequest request =
                new SendNotificationRequest(
                        "recipient@test.com",
                        NotificationChannel.EMAIL,
                        null,
                        "Hello from NotifyHub integration test",
                        null
                );

        NotificationResponse response =
                notificationService.send(savedUser, request);

        assertThat(response.getId()).isNotNull();

        Notification savedNotification =
                notificationRepository
                        .findById(response.getId())
                        .orElseThrow();

        assertThat(savedNotification.getRecipientUser().getId())
                .isEqualTo(savedUser.getId());

        assertThat(savedNotification.getRecipientAddress())
                .isEqualTo("recipient@test.com");

        assertThat(savedNotification.getChannel())
                .isEqualTo(NotificationChannel.EMAIL);

        assertThat(savedNotification.getPayload())
                .isEqualTo("Hello from NotifyHub integration test");

        assertThat(savedNotification.getStatus())
                .isEqualTo(NotificationStatus.QUEUED);

        assertThat(savedNotification.getRetryCount())
                .isEqualTo(0);

        Object message =
                rabbitTemplate.receiveAndConvert(
                        "notifyhub.email.queue"
                );

        assertThat(message).isNotNull();
    }
}