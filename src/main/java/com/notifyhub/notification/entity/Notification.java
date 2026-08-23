package com.notifyhub.notification.entity;

import com.notifyhub.user.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "notification",
        indexes = {

                // Used when fetching all notifications for a user,
                // ordered by newest notification first.
                @Index(
                        name = "idx_notification_user_created_at",
                        columnList = "recipient_user_id, created_at"
                ),

                // Used when filtering notifications by user + status.
                @Index(
                        name = "idx_notification_user_status",
                        columnList = "recipient_user_id, status"
                ),

                // Used when filtering notifications by user + channel.
                @Index(
                        name = "idx_notification_user_channel",
                        columnList = "recipient_user_id, channel"
                ),

                // Used when filtering notifications by user + status + channel.
                @Index(
                        name = "idx_notification_user_status_channel",
                        columnList = "recipient_user_id, status, channel"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    private AppUser recipientUser;

    @Column(nullable = false)
    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(length = 100)
    private String templateCode;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.QUEUED;

    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}