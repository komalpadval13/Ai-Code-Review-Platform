package com.codereview.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

public class ReviewDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SubmissionResponse {
        private Long id;
        private String fileName;
        private String originalFileName;
        private String language;
        private Long fileSize;
        private String status;
        private String currentStage;
        private Integer progressPercent;
        private Long projectId;
        private String projectName;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private ReviewSummary review;
        private MetricsSummary metrics;
        private PlagiarismSummary plagiarism;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReviewSummary {
        private Long id;
        private Integer totalIssues;
        private Integer criticalCount;
        private Integer warningCount;
        private Integer infoCount;
        private Double overallScore;
        private String summary;
        private List<FindingResponse> findings;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FindingResponse {
        private Long id;
        private String ruleId;
        private String title;
        private String description;
        private String severity;
        private Integer lineNumber;
        private Integer endLineNumber;
        private String codeSnippet;
        private String recommendation;
        private String fixedCode;
        private String source;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MetricsSummary {
        private Integer linesOfCode;
        private Integer blankLines;
        private Integer commentLines;
        private Integer codeLines;
        private Double commentRatio;
        private Integer cyclomaticComplexity;
        private Double maintainabilityIndex;
        private Integer numberOfMethods;
        private Integer numberOfClasses;
        private Double averageMethodLength;
        private Integer maxNestingDepth;
        private Integer numberOfImports;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlagiarismSummary {
        private Double similarityPercentage;
        private String matchingSections;
        private Long comparedSubmissionId;
        private String comparedFileName;
        private Boolean flagged;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProgressUpdate {
        private Long submissionId;
        private String stage;
        private Integer progressPercent;
        private String message;
        private String status;
    }
}
