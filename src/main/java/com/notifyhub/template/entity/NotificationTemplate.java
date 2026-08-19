package com.notifyhub.template.entity;

import com.notifyhub.notification.entity.NotificationChannel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "notification_template",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_template_code", columnNames = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;
}