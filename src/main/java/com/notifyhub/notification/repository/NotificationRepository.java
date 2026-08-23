package com.notifyhub.notification.repository;

import com.notifyhub.notification.entity.Notification;
import com.notifyhub.user.AppUser;
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
}