package com.notifyhub.audit.repository;

import com.notifyhub.audit.entity.NotificationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationAttemptRepository
        extends JpaRepository<NotificationAttempt, Long> {

    List<NotificationAttempt> findByNotificationIdOrderByAttemptNumberAsc(
            Long notificationId
    );
}