package com.supplybase.partners.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationReadStateRepository extends JpaRepository<NotificationReadState, Long> {
    Optional<NotificationReadState> findByUserIdAndOutboxId(Long userId, Long outboxId);
}
