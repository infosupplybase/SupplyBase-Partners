package com.supplybase.partners.kit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "kit_order_event")
@Getter
@Setter
@NoArgsConstructor
public class KitOrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kit_order_id", nullable = false)
    private Long kitOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private EventType eventType;

    @Column(name = "provider_event_id", length = 120)
    private String providerEventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public KitOrderEvent(Long kitOrderId, EventType eventType) {
        this.kitOrderId = kitOrderId;
        this.eventType = eventType;
    }

    public enum EventType { CREATED, PAYMENT_INITIATED, PAYMENT_SUCCEEDED, PAYMENT_FAILED, SHIPPED, DELIVERED, CANCELLED, REFUNDED }
}
