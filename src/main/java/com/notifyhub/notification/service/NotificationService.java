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
import com.notifyhub.preference.NotificationPreferenceService;
import com.notifyhub.template.service.TemplateService;
import com.notifyhub.user.AppUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationProducer notificationProducer;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;
    private final NotificationPreferenceService preferenceService;

    @Transactional
    public NotificationResponse send(
            AppUser appUser,
            SendNotificationRequest request
    ) {

        if (request.getIdempotencyKey() != null
                && !request.getIdempotencyKey().isBlank()) {

            return notificationRepository
                    .findByIdempotencyKey(request.getIdempotencyKey())
                    .map(NotificationResponse::from)
                    .orElseGet(() -> createAndSend(appUser, request));
        }

        return createAndSend(appUser, request);
    }

    private NotificationResponse createAndSend(
            AppUser appUser,
            SendNotificationRequest request
    ) {

        validateRecipient(
                request.getRecipientAddress(),
                request.getChannel()
        );

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

        boolean enabled = preferenceService.isEnabled(
                appUser,
                request.getChannel()
        );

        Notification notification = Notification.builder()
                .recipientUser(appUser)
                .recipientAddress(request.getRecipientAddress())
                .channel(request.getChannel())
                .templateCode(request.getTemplateCode())
                .payload(payload)
                .idempotencyKey(request.getIdempotencyKey())
                .status(
                        enabled
                                ? NotificationStatus.QUEUED
                                : NotificationStatus.SKIPPED
                )
                .retryCount(0)
                .build();

        Notification savedNotification =
                notificationRepository.save(notification);

        if (enabled
                && (savedNotification.getChannel() == NotificationChannel.EMAIL
                || savedNotification.getChannel() == NotificationChannel.SMS)) {

            NotificationEvent event = new NotificationEvent(
                    savedNotification.getId(),
                    savedNotification.getRecipientAddress(),
                    savedNotification.getChannel().name()
            );

            notificationProducer.publish(event);
        }

        return NotificationResponse.from(savedNotification);
    }

    private void validateRecipient(
            String recipientAddress,
            NotificationChannel channel
    ) {

        if (channel == NotificationChannel.EMAIL) {

            if (!recipientAddress.matches(
                    "^[A-Za-z0-9+_.-]+@(.+)$"
            )) {
                throw new IllegalArgumentException(
                        "Recipient address must be a valid email"
                );
            }

        } else if (channel == NotificationChannel.SMS) {

            if (!recipientAddress.matches(
                    "^\\+[1-9]\\d{7,14}$"
            )) {
                throw new IllegalArgumentException(
                        "Recipient address must be a valid phone number in E.164 format"
                );
            }
        }
    }

    public NotificationResponse getById(
            AppUser appUser,
            Long id
    ) {

        Notification notification = notificationRepository
                .findByIdAndRecipientUser(id, appUser)
                .orElseThrow(NotificationNotFoundException::new);

        return NotificationResponse.from(notification);
    }

    public Page<NotificationResponse> getAll(
            AppUser appUser,
            int page,
            int size,
            NotificationStatus status,
            NotificationChannel channel
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        /*
         * Every query is restricted to the currently logged-in user.
         * Additional filters for status and channel are added only
         * when they are provided in the request.
         */
        Specification<Notification> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(
                                root.get("recipientUser"),
                                appUser
                        );

        if (status != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("status"),
                                    status
                            )
            );
        }

        if (channel != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("channel"),
                                    channel
                            )
            );
        }

        return notificationRepository
                .findAll(specification, pageable)
                .map(NotificationResponse::from);
    }
}
