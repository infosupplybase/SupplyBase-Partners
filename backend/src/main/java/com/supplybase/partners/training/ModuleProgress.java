package com.supplybase.partners.training;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "module_progress")
@Getter
@Setter
@NoArgsConstructor
public class ModuleProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "module_id", nullable = false)
    private Long moduleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.NOT_STARTED;

    @Column(name = "completed_at")
    private Instant completedAt;

    public enum Status { NOT_STARTED, IN_PROGRESS, COMPLETED }
}
