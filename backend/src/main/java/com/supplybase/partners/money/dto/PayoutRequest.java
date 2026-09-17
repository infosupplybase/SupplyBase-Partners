package com.supplybase.partners.money.dto;

import jakarta.validation.constraints.Positive;

public record PayoutRequest(@Positive long amountPaise) {
}
