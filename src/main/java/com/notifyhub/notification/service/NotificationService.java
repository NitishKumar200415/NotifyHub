package com.notifyhub.notification.service;

import com.notifyhub.exception.NotificationNotFoundException;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.user.AppUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationResponse send(AppUser appUser,
                                     SendNotificationRequest request) {

        Notification notification = Notification.builder()
                .recipientUser(appUser)
                .recipientAddress(request.getRecipientAddress())
                .channel(request.getChannel())
                .templateCode(request.getTemplateCode())
                .payload(request.getPayload())
                .idempotencyKey(request.getIdempotencyKey())
                .status(NotificationStatus.QUEUED)
                .retryCount(0)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        return NotificationResponse.from(savedNotification);
    }

    public NotificationResponse getById(AppUser appUser, Long id) {

        Notification notification = notificationRepository
                .findByIdAndRecipientUser(id, appUser)
                .orElseThrow(NotificationNotFoundException::new);

        return NotificationResponse.from(notification);
    }

    public List<NotificationResponse> getAll(AppUser appUser) {

        return notificationRepository
                .findByRecipientUserOrderByCreatedAtDesc(appUser)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }
}