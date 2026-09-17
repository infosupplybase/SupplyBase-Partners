package com.supplybase.partners.kit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookKitOrderRequest(@NotBlank String deliveryAddressLine, @NotNull Long deliveryCityId,
                                   @NotBlank String idempotencyKey) {
}
