package com.supplybase.partners.training;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assessment")
@Getter
@Setter
@NoArgsConstructor
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "pass_score_percent", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private int passScorePercent = 70;

    @Column(name = "max_attempts", nullable = false, columnDefinition = "INT UNSIGNED")
    private int maxAttempts = 3;
}
