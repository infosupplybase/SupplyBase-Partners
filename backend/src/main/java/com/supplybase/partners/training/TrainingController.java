package com.supplybase.partners.training;

import com.supplybase.partners.common.domain.NotFoundException;
import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerService;
import com.supplybase.partners.training.dto.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/training")
@PreAuthorize("hasRole('PARTNER')")
public class TrainingController {

    private final TrainingService trainingService;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentOptionRepository optionRepository;
    private final PartnerService partnerService;
    private final CurrentUser currentUser;

    public TrainingController(TrainingService trainingService, AssessmentRepository assessmentRepository,
                               AssessmentQuestionRepository questionRepository, AssessmentOptionRepository optionRepository,
                               PartnerService partnerService, CurrentUser currentUser) {
        this.trainingService = trainingService;
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.partnerService = partnerService;
        this.currentUser = currentUser;
    }

    @GetMapping("/courses")
    public List<CourseResponse> courses() {
        return trainingService.requiredCourses().stream().map(CourseResponse::from).toList();
    }

    @GetMapping("/courses/{courseId}/modules")
    public List<ModuleResponse> modules(@PathVariable Long courseId) {
        return trainingService.modulesFor(courseId).stream().map(ModuleResponse::from).toList();
    }

    @GetMapping("/enrollments")
    public List<EnrollmentResponse> enrollments() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return trainingService.enrollmentsFor(partner.getId()).stream().map(EnrollmentResponse::from).toList();
    }

    @PostMapping("/courses/{courseId}/modules/{moduleId}/complete")
    public void completeModule(@PathVariable Long courseId, @PathVariable Long moduleId) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        trainingService.markModuleComplete(partner.getId(), courseId, moduleId);
    }

    @GetMapping("/courses/{courseId}/assessment")
    public List<AssessmentQuestionResponse> assessment(@PathVariable Long courseId) {
        Long assessmentId = assessmentRepository.findFirstByCourseId(courseId)
                .orElseThrow(() -> new NotFoundException("No assessment configured for this course.")).getId();
        return questionRepository.findAllByAssessmentIdOrderByDisplayOrder(assessmentId).stream()
                .map(q -> new AssessmentQuestionResponse(q.getId(), q.getQuestionText(),
                        optionRepository.findAllByQuestionId(q.getId()).stream()
                                .map(o -> new AssessmentQuestionResponse.OptionResponse(o.getId(), o.getOptionText())).toList()))
                .toList();
    }

    @PostMapping("/courses/{courseId}/assessment/submit")
    public TrainingService.AttemptResult submitAssessment(@PathVariable Long courseId, @RequestBody Map<Long, Long> answers) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        Long assessmentId = assessmentRepository.findFirstByCourseId(courseId)
                .orElseThrow(() -> new NotFoundException("No assessment configured for this course.")).getId();
        return trainingService.submitAssessment(partner.getId(), assessmentId, answers);
    }
}
