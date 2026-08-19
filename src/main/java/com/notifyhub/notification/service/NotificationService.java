package com.notifyhub.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.exception.NotificationNotFoundException;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.notification.messaging.NotificationEvent;
import com.notifyhub.notification.messaging.NotificationProducer;
import com.notifyhub.notification.repository.NotificationRepository;
import com.notifyhub.template.service.TemplateService;
import com.notifyhub.user.AppUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationProducer notificationProducer;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Transactional
    public NotificationResponse send(AppUser appUser,
                                     SendNotificationRequest request) {

        String payload = request.getPayload();

        /*
         * If a template is provided, treat the payload as JSON,
         * convert it into a Map, and render the template.
         *
         * Example payload:
         * {"name":"Nitish","orderId":"ORD-101"}
         *
         * Example template:
         * Hello {{name}}, your order {{orderId}} has been shipped.
         */
        if (request.getTemplateCode() != null
                && !request.getTemplateCode().isBlank()) {

            try {
                Map<String, String> payloadMap = objectMapper.readValue(
                        request.getPayload(),
                        new TypeReference<Map<String, String>>() {
                        }
                );

                payload = templateService.render(
                        request.getTemplateCode(),
                        payloadMap
                );

            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(
                        "Invalid template payload JSON",
                        e
                );
            }
        }

        Notification notification = Notification.builder()
                .recipientUser(appUser)
                .recipientAddress(request.getRecipientAddress())
                .channel(request.getChannel())
                .templateCode(request.getTemplateCode())
                .payload(payload)
                .idempotencyKey(request.getIdempotencyKey())
                .status(NotificationStatus.QUEUED)
                .retryCount(0)
                .build();

        Notification savedNotification =
                notificationRepository.save(notification);

        if (savedNotification.getChannel() == NotificationChannel.EMAIL
                || savedNotification.getChannel() == NotificationChannel.SMS) {

            NotificationEvent event = new NotificationEvent(
                    savedNotification.getId(),
                    savedNotification.getRecipientAddress(),
                    savedNotification.getChannel().name()
            );

            notificationProducer.publish(event);
        }

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