package com.supplybase.partners.training;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assessment_answer")
@Getter
@Setter
@NoArgsConstructor
public class AssessmentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "selected_option_id", nullable = false)
    private Long selectedOptionId;

    public AssessmentAnswer(Long attemptId, Long questionId, Long selectedOptionId) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
    }
}
