package com.notifyhub.preference;

import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationPreferenceRepository
        extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUserAndChannel(
            AppUser user,
            NotificationChannel channel
    );
}