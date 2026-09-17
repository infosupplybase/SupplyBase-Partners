package com.supplybase.partners.training;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "training_module")
@Getter
@Setter
@NoArgsConstructor
public class TrainingModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 16)
    private ContentType contentType;

    @Column(name = "content_body", columnDefinition = "MEDIUMTEXT")
    private String contentBody;

    @Column(name = "content_url", length = 500)
    private String contentUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public enum ContentType { TEXT, VIDEO }
}
