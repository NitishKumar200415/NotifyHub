package com.notifyhub.notification.repository;

import com.notifyhub.notification.entity.Notification;
import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.notification.entity.NotificationStatus;
import com.notifyhub.user.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long>,
        JpaSpecificationExecutor<Notification> {

    Optional<Notification> findByIdAndRecipientUser(
            Long id,
            AppUser recipientUser
    );

    Optional<Notification> findByIdempotencyKey(
            String idempotencyKey
    );

    Page<Notification> findByRecipientUserOrderByCreatedAtDesc(
            AppUser recipientUser,
            Pageable pageable
    );

    Page<Notification> findByRecipientUserAndStatusOrderByCreatedAtDesc(
            AppUser recipientUser,
            NotificationStatus status,
            Pageable pageable
    );

    Page<Notification> findByRecipientUserAndChannelOrderByCreatedAtDesc(
            AppUser recipientUser,
            NotificationChannel channel,
            Pageable pageable
    );

    Page<Notification>
    findByRecipientUserAndStatusAndChannelOrderByCreatedAtDesc(
            AppUser recipientUser,
            NotificationStatus status,
            NotificationChannel channel,
            Pageable pageable
    );

    // Statistics

    long countByRecipientUser(
            AppUser recipientUser
    );

    long countByRecipientUserAndStatus(
            AppUser recipientUser,
            NotificationStatus status
    );
}