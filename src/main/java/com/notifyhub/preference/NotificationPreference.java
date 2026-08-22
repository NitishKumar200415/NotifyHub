package com.notifyhub.preference;

import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.user.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "notification_preference",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"user_id", "channel"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
}