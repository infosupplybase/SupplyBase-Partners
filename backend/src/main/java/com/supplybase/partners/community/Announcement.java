package com.supplybase.partners.community;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "announcement")
@Getter
@Setter
@NoArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String body;

    @Column(name = "published_by_user_id", nullable = false)
    private Long publishedByUserId;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(nullable = false)
    private boolean active = true;

    @PrePersist
    void prePersist() {
        if (publishedAt == null) publishedAt = Instant.now();
    }
}
