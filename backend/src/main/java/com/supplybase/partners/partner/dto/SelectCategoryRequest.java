package com.supplybase.partners.partner.dto;

import jakarta.validation.constraints.NotNull;

public record SelectCategoryRequest(@NotNull Long categoryId) {
}
