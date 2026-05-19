package com.codereview.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions", indexes = {
    @Index(name = "idx_submission_project", columnList = "project_id"),
    @Index(name = "idx_submission_user", columnList = "user_id"),
    @Index(name = "idx_submission_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(nullable = false)
    private Long fileSize;

    @Column(columnDefinition = "LONGTEXT")
    private String sourceCode;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.UPLOADED;

    @Column(length = 50)
    private String currentStage;

    @Column
    @Builder.Default
    private Integer progressPercent = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private Review review;

    @OneToOne(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private CodeMetrics metrics;

    @OneToOne(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private PlagiarismResult plagiarismResult;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime completedAt = null;

    public enum SubmissionStatus {
        UPLOADED, PROCESSING, ANALYZING, AI_REVIEWING, PLAGIARISM_CHECKING,
        GENERATING_REPORT, COMPLETED, FAILED
    }
}
