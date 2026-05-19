package com.codereview.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "code_metrics", indexes = {
    @Index(name = "idx_metrics_submission", columnList = "submission_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodeMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private Submission submission;

    @Column(nullable = false)
    @Builder.Default
    private Integer linesOfCode = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer blankLines = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer commentLines = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer codeLines = 0;

    @Column(nullable = false)
    @Builder.Default
    private Double commentRatio = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer cyclomaticComplexity = 0;

    @Column(nullable = false)
    @Builder.Default
    private Double maintainabilityIndex = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer numberOfMethods = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer numberOfClasses = 0;

    @Column(nullable = false)
    @Builder.Default
    private Double averageMethodLength = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxNestingDepth = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer numberOfImports = 0;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
