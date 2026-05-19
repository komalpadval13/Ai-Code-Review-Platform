package com.codereview.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_review_submission", columnList = "submission_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private Submission submission;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalIssues = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer criticalCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer warningCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer infoCount = 0;

    @Column
    @Builder.Default
    private Double overallScore = 0.0;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Finding> findings = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ReviewSource source = ReviewSource.STATIC;

    public enum ReviewSource {
        STATIC, AI, COMBINED
    }
}
