package com.supplybase.partners.training.dto;

import com.supplybase.partners.training.TrainingCourse;

public record CourseResponse(Long id, String title, String description) {
    public static CourseResponse from(TrainingCourse c) {
        return new CourseResponse(c.getId(), c.getTitle(), c.getDescription());
    }
}
