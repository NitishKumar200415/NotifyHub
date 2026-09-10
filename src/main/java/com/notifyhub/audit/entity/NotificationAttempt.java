package com.notifyhub.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "notification_attempt",
        indexes = {
                @Index(
                        name = "idx_notification_attempt_notification_id",
                        columnList = "notification_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "notification_id",
            nullable = false
    )
    private Long notificationId;

    @Column(
            name = "attempt_number",
            nullable = false
    )
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private AttemptStatus status;

    @Column(
            name = "error_message",
            columnDefinition = "TEXT"
    )
    private String errorMessage;

    @Column(
            name = "attempted_at",
            nullable = false,
            updatable = false
    )
    private Instant attemptedAt;

    @PrePersist
    protected void onCreate() {
        attemptedAt = Instant.now();
    }

    public enum AttemptStatus {
        SUCCESS,
        FAILURE
    }
}