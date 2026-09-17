package com.supplybase.partners.training;

import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.common.domain.NotFoundException;
import com.supplybase.partners.partner.OnboardingStage;
import com.supplybase.partners.partner.PartnerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Progress is only ever written by an explicit call from here -- opening a
 * lesson page in the frontend is not, by itself, treated as "watched". Grading
 * happens entirely server-side from the stored correct options, never from a
 * client-reported score.
 */
@Service
public class TrainingService {

    private final TrainingCourseRepository courseRepository;
    private final TrainingModuleRepository moduleRepository;
    private final TrainingEnrollmentRepository enrollmentRepository;
    private final ModuleProgressRepository moduleProgressRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentOptionRepository optionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final PartnerService partnerService;

    public TrainingService(TrainingCourseRepository courseRepository, TrainingModuleRepository moduleRepository,
                            TrainingEnrollmentRepository enrollmentRepository, ModuleProgressRepository moduleProgressRepository,
                            AssessmentRepository assessmentRepository, AssessmentQuestionRepository questionRepository,
                            AssessmentOptionRepository optionRepository, AssessmentAttemptRepository attemptRepository,
                            AssessmentAnswerRepository answerRepository, PartnerService partnerService) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.moduleProgressRepository = moduleProgressRepository;
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.partnerService = partnerService;
    }

    public List<TrainingCourse> requiredCourses() {
        return courseRepository.findAllByRequiredTrueAndActiveTrue();
    }

    public List<TrainingModule> modulesFor(Long courseId) {
        return moduleRepository.findAllByCourseIdOrderByDisplayOrder(courseId);
    }

    @Transactional
    public TrainingEnrollment getOrCreateEnrollment(Long partnerId, Long courseId) {
        return enrollmentRepository.findByPartnerIdAndCourseId(partnerId, courseId).orElseGet(() -> {
            TrainingEnrollment e = new TrainingEnrollment();
            e.setPartnerId(partnerId);
            e.setCourseId(courseId);
            e.setStatus(TrainingEnrollment.Status.NOT_STARTED);
            return enrollmentRepository.save(e);
        });
    }

    public List<TrainingEnrollment> enrollmentsFor(Long partnerId) {
        return enrollmentRepository.findAllByPartnerId(partnerId);
    }

    public List<ModuleProgress> moduleProgressFor(Long enrollmentId) {
        return moduleProgressRepository.findAllByEnrollmentId(enrollmentId);
    }

    @Transactional
    public ModuleProgress markModuleComplete(Long partnerId, Long courseId, Long moduleId) {
        TrainingEnrollment enrollment = getOrCreateEnrollment(partnerId, courseId);
        if (enrollment.getStatus() == TrainingEnrollment.Status.NOT_STARTED) {
            enrollment.setStatus(TrainingEnrollment.Status.IN_PROGRESS);
            enrollment.setStartedAt(Instant.now());
            enrollmentRepository.save(enrollment);
        }
        ModuleProgress progress = moduleProgressRepository.findByEnrollmentIdAndModuleId(enrollment.getId(), moduleId)
                .orElseGet(ModuleProgress::new);
        progress.setEnrollmentId(enrollment.getId());
        progress.setModuleId(moduleId);
        progress.setStatus(ModuleProgress.Status.COMPLETED);
        progress.setCompletedAt(Instant.now());
        return moduleProgressRepository.save(progress);
    }

    public record AttemptResult(int scorePercent, boolean passed, int attemptNumber, int attemptsRemaining) {}

    /** answers: questionId -> selectedOptionId. Grading and the score are entirely server-computed. */
    @Transactional
    public AttemptResult submitAssessment(Long partnerId, Long assessmentId, Map<Long, Long> answers) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NotFoundException("Assessment not found."));
        List<AssessmentAttempt> previous = attemptRepository.findAllByAssessmentIdAndPartnerIdOrderByAttemptNumberDesc(assessmentId, partnerId);
        if (previous.size() >= assessment.getMaxAttempts() && previous.stream().noneMatch(AssessmentAttempt::isPassed)) {
            throw new BadRequestException("Maximum attempts reached for this assessment.");
        }
        if (previous.stream().anyMatch(AssessmentAttempt::isPassed)) {
            throw new BadRequestException("You have already passed this assessment.");
        }

        List<AssessmentQuestion> questions = questionRepository.findAllByAssessmentIdOrderByDisplayOrder(assessmentId);
        int correctCount = 0;
        for (AssessmentQuestion q : questions) {
            Long selected = answers.get(q.getId());
            if (selected == null) continue;
            boolean isCorrect = optionRepository.findAllByQuestionId(q.getId()).stream()
                    .anyMatch(o -> o.getId().equals(selected) && o.isCorrect());
            if (isCorrect) correctCount++;
        }
        int scorePercent = questions.isEmpty() ? 0 : (correctCount * 100) / questions.size();
        boolean passed = scorePercent >= assessment.getPassScorePercent();

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessmentId(assessmentId);
        attempt.setPartnerId(partnerId);
        attempt.setAttemptNumber(previous.size() + 1);
        attempt.setScorePercent(scorePercent);
        attempt.setPassed(passed);
        attemptRepository.save(attempt);

        answers.forEach((questionId, optionId) -> answerRepository.save(new AssessmentAnswer(attempt.getId(), questionId, optionId)));

        if (passed) {
            TrainingEnrollment enrollment = getOrCreateEnrollment(partnerId, assessment.getCourseId());
            enrollment.setStatus(TrainingEnrollment.Status.COMPLETED);
            enrollment.setCompletedAt(Instant.now());
            enrollmentRepository.save(enrollment);
            checkAndAdvanceIfAllRequiredComplete(partnerId);
        }

        int attemptsRemaining = Math.max(0, assessment.getMaxAttempts() - (previous.size() + 1));
        return new AttemptResult(scorePercent, passed, attempt.getAttemptNumber(), attemptsRemaining);
    }

    private void checkAndAdvanceIfAllRequiredComplete(Long partnerId) {
        List<TrainingCourse> required = requiredCourses();
        boolean allComplete = required.stream().allMatch(c ->
                enrollmentRepository.findByPartnerIdAndCourseId(partnerId, c.getId())
                        .map(e -> e.getStatus() == TrainingEnrollment.Status.COMPLETED)
                        .orElse(false));
        if (allComplete) {
            partnerService.advanceStageIfCurrent(partnerId, OnboardingStage.TRAINING, OnboardingStage.ACTIVATION_REVIEW);
        }
    }
}
