package com.supplybase.partners.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notification_read_state")
@Getter
@Setter
@NoArgsConstructor
public class NotificationReadState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "outbox_id", nullable = false)
    private Long outboxId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    @PrePersist
    void prePersist() {
        if (readAt == null) readAt = Instant.now();
    }
}
