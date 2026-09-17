package com.supplybase.partners.kit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "kit_order")
@Getter
@Setter
@NoArgsConstructor
public class KitOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "kit_id", nullable = false)
    private Long kitId;

    @Column(name = "delivery_address_line", nullable = false, length = 500)
    private String deliveryAddressLine;

    @Column(name = "delivery_city_id", nullable = false)
    private Long deliveryCityId;

    @Column(name = "price_paise", nullable = false)
    private long pricePaise;

    @Column(name = "fees_paise", nullable = false)
    private long feesPaise;

    @Column(name = "total_paise", nullable = false)
    private long totalPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status = Status.QUOTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider", nullable = false, length = 24)
    private PaymentProvider paymentProvider = PaymentProvider.DEV_SIMULATOR;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public enum Status { QUOTE, PENDING_PAYMENT, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED, PAYMENT_FAILED }

    public enum PaymentProvider { DEV_SIMULATOR }
}
