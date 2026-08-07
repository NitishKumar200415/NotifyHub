package com.notifyhub.notification;

import com.notifyhub.exception.NotificationNotFoundException;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.notification.service.EmailSenderService;
import com.notifyhub.notification.service.NotificationService;
import com.notifyhub.user.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailSenderService emailSenderService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void send_ShouldCreateNotificationSuccessfully() {

        AppUser user = AppUser.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientAddress("john@example.com");
        request.setChannel(NotificationChannel.EMAIL);
        request.setTemplateCode("WELCOME");
        request.setPayload("{\"name\":\"John\"}");
        request.setIdempotencyKey("abc-123");

        Notification notification = Notification.builder()
                .id(1L)
                .recipientUser(user)
                .recipientAddress(request.getRecipientAddress())
                .channel(request.getChannel())
                .templateCode(request.getTemplateCode())
                .payload(request.getPayload())
                .idempotencyKey(request.getIdempotencyKey())
                .status(NotificationStatus.QUEUED)
                .retryCount(0)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);

        when(emailSenderService.sendEmail(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(true);

        NotificationResponse response =
                notificationService.send(user, request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(NotificationStatus.SENT, response.getStatus());
        assertEquals("john@example.com", response.getRecipientAddress());

        verify(notificationRepository, times(2))
                .save(any(Notification.class));

        verify(emailSenderService).sendEmail(
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void send_ShouldMarkNotificationAsFailed_WhenEmailSendingFails() {

        AppUser user = AppUser.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientAddress("john@example.com");
        request.setChannel(NotificationChannel.EMAIL);
        request.setTemplateCode("WELCOME");
        request.setPayload("{\"name\":\"John\"}");
        request.setIdempotencyKey("abc-123");

        Notification notification = Notification.builder()
                .id(1L)
                .recipientUser(user)
                .recipientAddress(request.getRecipientAddress())
                .channel(request.getChannel())
                .templateCode(request.getTemplateCode())
                .payload(request.getPayload())
                .idempotencyKey(request.getIdempotencyKey())
                .status(NotificationStatus.QUEUED)
                .retryCount(0)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);

        when(emailSenderService.sendEmail(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(false);

        NotificationResponse response =
                notificationService.send(user, request);

        assertEquals(NotificationStatus.FAILED, response.getStatus());

        verify(notificationRepository, times(2))
                .save(any(Notification.class));

        verify(emailSenderService).sendEmail(
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void getById_ShouldReturnNotification() {

        AppUser user = AppUser.builder()
                .id(1L)
                .build();

        Notification notification = Notification.builder()
                .id(1L)
                .recipientUser(user)
                .recipientAddress("john@example.com")
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.SENT)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findByIdAndRecipientUser(1L, user))
                .thenReturn(Optional.of(notification));

        NotificationResponse response =
                notificationService.getById(user, 1L);

        assertEquals(1L, response.getId());
        assertEquals(NotificationStatus.SENT, response.getStatus());
    }

    @Test
    void getById_ShouldThrowException_WhenNotificationNotFound() {

        AppUser user = AppUser.builder()
                .id(1L)
                .build();

        when(notificationRepository.findByIdAndRecipientUser(99L, user))
                .thenReturn(Optional.empty());

        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.getById(user, 99L)
        );
    }

    @Test
    void getAll_ShouldReturnNotifications() {

        AppUser user = AppUser.builder()
                .id(1L)
                .build();

        Notification notification = Notification.builder()
                .id(1L)
                .recipientUser(user)
                .recipientAddress("john@example.com")
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.SENT)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findByRecipientUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(notification));

        List<NotificationResponse> responses =
                notificationService.getAll(user);

        assertEquals(1, responses.size());
        assertEquals("john@example.com",
                responses.get(0).getRecipientAddress());
    }
}