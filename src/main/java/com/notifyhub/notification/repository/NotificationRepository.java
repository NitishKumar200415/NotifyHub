package com.notifyhub.notification.repository;

import com.notifyhub.notification.entity.Notification;
import com.notifyhub.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdAndRecipientUser(Long id, AppUser recipientUser);

    List<Notification> findByRecipientUserOrderByCreatedAtDesc(AppUser recipientUser);

}