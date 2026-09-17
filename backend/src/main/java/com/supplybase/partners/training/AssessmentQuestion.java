package com.supplybase.partners.training;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assessment_question")
@Getter
@Setter
@NoArgsConstructor
public class AssessmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
