package com.supplybase.partners.partner.dto;

import jakarta.validation.constraints.NotNull;

public record SelectCityRequest(@NotNull Long cityId) {
}
