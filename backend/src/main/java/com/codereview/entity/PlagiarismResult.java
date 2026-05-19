package com.codereview.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "plagiarism_results", indexes = {
    @Index(name = "idx_plagiarism_submission", columnList = "submission_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlagiarismResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private Submission submission;

    @Column(nullable = false)
    @Builder.Default
    private Double similarityPercentage = 0.0;

    @Column(columnDefinition = "LONGTEXT")
    private String fingerprints;

    @Column(columnDefinition = "TEXT")
    private String matchingSections;

    @Column
    private Long comparedSubmissionId;

    @Column(length = 255)
    private String comparedFileName;

    @Column(nullable = false)
    @Builder.Default
    private Boolean flagged = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
