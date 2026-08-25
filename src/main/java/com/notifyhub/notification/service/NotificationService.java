package com.notifyhub.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.exception.NotificationNotFoundException;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.NotificationStatsResponse;
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


    // =========================================================
    // SEND NOTIFICATION
    // =========================================================

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
         * If a template is provided, treat the payload as JSON
         * and render the notification template.
         */
        if (request.getTemplateCode() != null
                && !request.getTemplateCode().isBlank()) {

            try {

                Map<String, String> payloadMap =
                        objectMapper.readValue(
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

        /*
         * Check whether the user has enabled this
         * notification channel.
         */
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

        /*
         * Publish enabled notifications to RabbitMQ.
         *
         * EMAIL -> email routing key
         * SMS   -> sms routing key
         * PUSH  -> push routing key
         */
        if (enabled) {

            NotificationEvent event =
                    new NotificationEvent(
                            savedNotification.getId(),
                            savedNotification.getRecipientAddress(),
                            savedNotification.getChannel().name()
                    );

            notificationProducer.publish(event);
        }

        return NotificationResponse.from(savedNotification);
    }


    // =========================================================
    // RETRY NOTIFICATION
    // =========================================================

    @Transactional
    public NotificationResponse retry(
            AppUser appUser,
            Long id
    ) {

        Notification notification =
                notificationRepository
                        .findByIdAndRecipientUser(id, appUser)
                        .orElseThrow(
                                NotificationNotFoundException::new
                        );

        /*
         * Only failed notifications can be retried.
         */
        if (notification.getStatus()
                != NotificationStatus.FAILED
                && notification.getStatus()
                != NotificationStatus.DEAD_LETTERED) {

            throw new IllegalStateException(
                    "Only FAILED or DEAD_LETTERED notifications can be retried"
            );
        }

        /*
         * Reset the notification for a new retry cycle.
         */
        notification.setStatus(NotificationStatus.QUEUED);
        notification.setRetryCount(0);

        Notification savedNotification =
                notificationRepository.save(notification);

        /*
         * Send the notification back through RabbitMQ.
         */
        NotificationEvent event =
                new NotificationEvent(
                        savedNotification.getId(),
                        savedNotification.getRecipientAddress(),
                        savedNotification.getChannel().name()
                );

        notificationProducer.publish(event);

        return NotificationResponse.from(savedNotification);
    }


    // =========================================================
    // VALIDATE RECIPIENT
    // =========================================================

    private void validateRecipient(
            String recipientAddress,
            NotificationChannel channel
    ) {

        if (channel == NotificationChannel.EMAIL) {

            if (recipientAddress == null
                    || !recipientAddress.matches(
                    "^[A-Za-z0-9+_.-]+@(.+)$"
            )) {

                throw new IllegalArgumentException(
                        "Recipient address must be a valid email"
                );
            }

        } else if (channel == NotificationChannel.SMS) {

            if (recipientAddress == null
                    || !recipientAddress.matches(
                    "^\\+[1-9]\\d{7,14}$"
            )) {

                throw new IllegalArgumentException(
                        "Recipient address must be a valid phone number in E.164 format"
                );
            }

        } else if (channel == NotificationChannel.PUSH) {

            /*
             * For PUSH notifications, recipientAddress
             * represents the device token.
             */
            if (recipientAddress == null
                    || recipientAddress.isBlank()) {

                throw new IllegalArgumentException(
                        "Device token must not be blank"
                );
            }
        }
    }


    // =========================================================
    // GET NOTIFICATION BY ID
    // =========================================================

    public NotificationResponse getById(
            AppUser appUser,
            Long id
    ) {

        Notification notification =
                notificationRepository
                        .findByIdAndRecipientUser(id, appUser)
                        .orElseThrow(
                                NotificationNotFoundException::new
                        );

        return NotificationResponse.from(notification);
    }


    // =========================================================
    // GET ALL NOTIFICATIONS
    // =========================================================

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
         * Every query is restricted to the currently
         * authenticated user.
         */
        Specification<Notification> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(
                                root.get("recipientUser"),
                                appUser
                        );

        /*
         * Filter by status if provided.
         */
        if (status != null) {

            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("status"),
                                    status
                            )
            );
        }

        /*
         * Filter by channel if provided.
         */
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


    // =========================================================
    // NOTIFICATION STATISTICS
    // =========================================================

    public NotificationStatsResponse getStats(
            AppUser appUser
    ) {

        long total =
                notificationRepository
                        .countByRecipientUser(appUser);

        long queued =
                notificationRepository
                        .countByRecipientUserAndStatus(
                                appUser,
                                NotificationStatus.QUEUED
                        );

        long sent =
                notificationRepository
                        .countByRecipientUserAndStatus(
                                appUser,
                                NotificationStatus.SENT
                        );

        long failed =
                notificationRepository
                        .countByRecipientUserAndStatus(
                                appUser,
                                NotificationStatus.FAILED
                        );

        long deadLettered =
                notificationRepository
                        .countByRecipientUserAndStatus(
                                appUser,
                                NotificationStatus.DEAD_LETTERED
                        );

        long skipped =
                notificationRepository
                        .countByRecipientUserAndStatus(
                                appUser,
                                NotificationStatus.SKIPPED
                        );

        return new NotificationStatsResponse(
                total,
                queued,
                sent,
                failed,
                deadLettered,
                skipped
        );
    }
}