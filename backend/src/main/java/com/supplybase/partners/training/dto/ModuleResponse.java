package com.supplybase.partners.training.dto;

import com.supplybase.partners.training.TrainingModule;

public record ModuleResponse(Long id, String title, String contentType, String contentBody, String contentUrl) {
    public static ModuleResponse from(TrainingModule m) {
        return new ModuleResponse(m.getId(), m.getTitle(), m.getContentType().name(), m.getContentBody(), m.getContentUrl());
    }
}
