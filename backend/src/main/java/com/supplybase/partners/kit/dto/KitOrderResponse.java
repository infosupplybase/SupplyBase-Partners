package com.supplybase.partners.kit.dto;

import com.supplybase.partners.kit.KitOrder;

import java.time.Instant;

public record KitOrderResponse(Long id, Long kitId, String deliveryAddressLine, Long deliveryCityId,
                                long pricePaise, long feesPaise, long totalPaise, String status,
                                Instant createdAt, Instant updatedAt) {
    public static KitOrderResponse from(KitOrder o) {
        return new KitOrderResponse(o.getId(), o.getKitId(), o.getDeliveryAddressLine(), o.getDeliveryCityId(),
                o.getPricePaise(), o.getFeesPaise(), o.getTotalPaise(), o.getStatus().name(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
