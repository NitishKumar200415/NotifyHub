package com.notifyhub.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.exception.NotificationNotFoundException;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.messaging.NotificationProducer;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.notification.service.NotificationService;
import com.notifyhub.preference.NotificationPreferenceService;
import com.notifyhub.template.service.TemplateService;
import com.notifyhub.user.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
    private NotificationProducer notificationProducer;

    @Mock
    private TemplateService templateService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationPreferenceService preferenceService;

    @InjectMocks
    private NotificationService notificationService;


    @Test
    void send_ShouldCreateAndQueueNotificationSuccessfully() {

        AppUser user = AppUser.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        SendNotificationRequest request =
                new SendNotificationRequest();

        request.setRecipientAddress("john@example.com");
        request.setChannel(NotificationChannel.EMAIL);
        request.setPayload("Hello John");
        request.setIdempotencyKey("abc-123");

        when(preferenceService.isEnabled(
                user,
                NotificationChannel.EMAIL
        )).thenReturn(true);

        Notification savedNotification =
                Notification.builder()
                        .id(1L)
                        .recipientUser(user)
                        .recipientAddress(
                                request.getRecipientAddress()
                        )
                        .channel(request.getChannel())
                        .payload(request.getPayload())
                        .idempotencyKey(
                                request.getIdempotencyKey()
                        )
                        .status(NotificationStatus.QUEUED)
                        .retryCount(0)
                        .createdAt(Instant.now())
                        .build();

        when(notificationRepository.save(
                any(Notification.class)
        )).thenReturn(savedNotification);

        NotificationResponse response =
                notificationService.send(user, request);

        assertNotNull(response);

        assertEquals(
                1L,
                response.getId()
        );

        assertEquals(
                NotificationStatus.QUEUED,
                response.getStatus()
        );

        assertEquals(
                "john@example.com",
                response.getRecipientAddress()
        );

        verify(notificationRepository)
                .save(any(Notification.class));

        verify(notificationProducer)
                .publish(any());

        verifyNoInteractions(templateService);
    }


    @Test
    void send_ShouldMarkNotificationAsSkipped_WhenPreferenceDisabled() {

        AppUser user = AppUser.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        SendNotificationRequest request =
                new SendNotificationRequest();

        request.setRecipientAddress("john@example.com");
        request.setChannel(NotificationChannel.EMAIL);
        request.setPayload("Hello John");

        when(preferenceService.isEnabled(
                user,
                NotificationChannel.EMAIL
        )).thenReturn(false);

        Notification skippedNotification =
                Notification.builder()
                        .id(1L)
                        .recipientUser(user)
                        .recipientAddress(
                                request.getRecipientAddress()
                        )
                        .channel(request.getChannel())
                        .payload(request.getPayload())
                        .status(NotificationStatus.SKIPPED)
                        .retryCount(0)
                        .createdAt(Instant.now())
                        .build();

        when(notificationRepository.save(
                any(Notification.class)
        )).thenReturn(skippedNotification);

        NotificationResponse response =
                notificationService.send(user, request);

        assertEquals(
                NotificationStatus.SKIPPED,
                response.getStatus()
        );

        verify(notificationRepository)
                .save(any(Notification.class));

        verify(notificationProducer, never())
                .publish(any());
    }


    @Test
    void getById_ShouldReturnNotification() {

        AppUser user = AppUser.builder()
                .id(1L)
                .build();

        Notification notification =
                Notification.builder()
                        .id(1L)
                        .recipientUser(user)
                        .recipientAddress("john@example.com")
                        .channel(NotificationChannel.EMAIL)
                        .status(NotificationStatus.SENT)
                        .createdAt(Instant.now())
                        .build();

        when(notificationRepository.findByIdAndRecipientUser(
                1L,
                user
        )).thenReturn(Optional.of(notification));

        NotificationResponse response =
                notificationService.getById(user, 1L);

        assertEquals(
                1L,
                response.getId()
        );

        assertEquals(
                NotificationStatus.SENT,
                response.getStatus()
        );
    }


    @Test
    void getById_ShouldThrowException_WhenNotificationNotFound() {

        AppUser user = AppUser.builder()
                .id(1L)
                .build();

        when(notificationRepository.findByIdAndRecipientUser(
                99L,
                user
        )).thenReturn(Optional.empty());

        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.getById(
                        user,
                        99L
                )
        );
    }


    @Test
    void getAll_ShouldReturnNotifications() {

        AppUser user = AppUser.builder()
                .id(1L)
                .build();

        Notification notification =
                Notification.builder()
                        .id(1L)
                        .recipientUser(user)
                        .recipientAddress("john@example.com")
                        .channel(NotificationChannel.EMAIL)
                        .status(NotificationStatus.SENT)
                        .createdAt(Instant.now())
                        .build();

        Page<Notification> notificationPage =
                new PageImpl<>(
                        List.of(notification)
                );

        when(notificationRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(notificationPage);

        Page<NotificationResponse> responses =
                notificationService.getAll(
                        user,
                        0,
                        10,
                        null,
                        null
                );

        assertEquals(
                1,
                responses.getContent().size()
        );

        assertEquals(
                "john@example.com",
                responses.getContent()
                        .get(0)
                        .getRecipientAddress()
        );
    }
}