package com.supplybase.partners.training;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/training")
@PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
public class AdminTrainingController {

    private final TrainingCourseRepository courseRepository;
    private final TrainingModuleRepository moduleRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentOptionRepository optionRepository;

    public AdminTrainingController(TrainingCourseRepository courseRepository, TrainingModuleRepository moduleRepository,
                                    AssessmentRepository assessmentRepository, AssessmentQuestionRepository questionRepository,
                                    AssessmentOptionRepository optionRepository) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    @PostMapping("/courses")
    public TrainingCourse createCourse(@RequestBody Map<String, Object> body) {
        TrainingCourse c = new TrainingCourse();
        c.setTitle(body.get("title").toString());
        c.setDescription((String) body.get("description"));
        if (body.get("categoryId") != null) c.setCategoryId(Long.valueOf(body.get("categoryId").toString()));
        c.setRequired(!body.containsKey("required") || Boolean.parseBoolean(body.get("required").toString()));
        return courseRepository.save(c);
    }

    @PostMapping("/courses/{courseId}/modules")
    public TrainingModule addModule(@PathVariable Long courseId, @RequestBody Map<String, Object> body) {
        TrainingModule m = new TrainingModule();
        m.setCourseId(courseId);
        m.setTitle(body.get("title").toString());
        m.setContentType(TrainingModule.ContentType.valueOf(body.get("contentType").toString()));
        m.setContentBody((String) body.get("contentBody"));
        m.setContentUrl((String) body.get("contentUrl"));
        m.setDisplayOrder(body.containsKey("displayOrder") ? Integer.parseInt(body.get("displayOrder").toString()) : 0);
        return moduleRepository.save(m);
    }

    @PostMapping("/courses/{courseId}/assessment")
    public Assessment createAssessment(@PathVariable Long courseId, @RequestBody Map<String, Object> body) {
        Assessment a = new Assessment();
        a.setCourseId(courseId);
        a.setTitle(body.get("title").toString());
        if (body.containsKey("passScorePercent")) a.setPassScorePercent(Integer.parseInt(body.get("passScorePercent").toString()));
        if (body.containsKey("maxAttempts")) a.setMaxAttempts(Integer.parseInt(body.get("maxAttempts").toString()));
        return assessmentRepository.save(a);
    }

    @PostMapping("/assessments/{assessmentId}/questions")
    @SuppressWarnings("unchecked")
    public AssessmentQuestion addQuestion(@PathVariable Long assessmentId, @RequestBody Map<String, Object> body) {
        AssessmentQuestion q = new AssessmentQuestion();
        q.setAssessmentId(assessmentId);
        q.setQuestionText(body.get("questionText").toString());
        q.setDisplayOrder(body.containsKey("displayOrder") ? Integer.parseInt(body.get("displayOrder").toString()) : 0);
        questionRepository.save(q);

        var options = (java.util.List<Map<String, Object>>) body.get("options");
        if (options != null) {
            for (Map<String, Object> opt : options) {
                AssessmentOption o = new AssessmentOption();
                o.setQuestionId(q.getId());
                o.setOptionText(opt.get("text").toString());
                o.setCorrect(Boolean.TRUE.equals(opt.get("correct")));
                optionRepository.save(o);
            }
        }
        return q;
    }
}
