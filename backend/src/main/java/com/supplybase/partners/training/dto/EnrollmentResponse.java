package com.supplybase.partners.training.dto;

import com.supplybase.partners.training.TrainingEnrollment;

import java.time.Instant;

public record EnrollmentResponse(Long courseId, String status, Instant startedAt, Instant completedAt) {
    public static EnrollmentResponse from(TrainingEnrollment e) {
        return new EnrollmentResponse(e.getCourseId(), e.getStatus().name(), e.getStartedAt(), e.getCompletedAt());
    }
}
