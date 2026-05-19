package com.codereview.service;

import com.codereview.dto.ReviewDTO;
import com.codereview.entity.*;
import com.codereview.exception.BadRequestException;
import com.codereview.exception.ResourceNotFoundException;
import com.codereview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StaticAnalysisService staticAnalysisService;
    private final MetricsService metricsService;
    private final AIReviewService aiReviewService;
    private final PlagiarismService plagiarismService;
    private final AuditService auditService;

    @Value("${app.upload.directory}")
    private String uploadDir;

    @Value("${app.upload.allowed-extensions}")
    private String allowedExtensions;

    @Transactional
    public ReviewDTO.SubmissionResponse uploadAndProcess(Long userId, Long projectId, MultipartFile file) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Project project = projectRepository.findById(Objects.requireNonNull(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        if (!project.getOwner().getId().equals(userId))
            throw new BadRequestException("No permission to upload to this project");

        validateFile(file);
        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        String language = detectLanguage(extension);
        String storedName = UUID.randomUUID() + extension;

        try {
            Path uploadPath = Paths.get(uploadDir, String.valueOf(projectId));
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String sourceCode = Files.readString(filePath);

            Submission submission = Submission.builder()
                    .fileName(storedName).originalFileName(originalName)
                    .language(language).fileSize(file.getSize())
                    .sourceCode(sourceCode).filePath(filePath.toString())
                    .status(Submission.SubmissionStatus.UPLOADED)
                    .currentStage("Uploading").progressPercent(0)
                    .project(project).user(user).build();
            submission = submissionRepository.save(Objects.requireNonNull(submission));

            auditService.log(userId, user.getUsername(), "UPLOAD",
                    "Uploaded file: " + originalName, "Submission", submission.getId());

            processSubmissionAsync(submission.getId());
            return mapToResponse(submission);
        } catch (IOException e) {
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }

    @Async
    public void processSubmissionAsync(Long submissionId) {
        try {
            Submission submission = submissionRepository.findById(Objects.requireNonNull(submissionId)).orElseThrow();
            String sourceCode = submission.getSourceCode();

            updateProgress(submission, "Parsing", 10, "PROCESSING");
            Thread.sleep(500);

            updateProgress(submission, "Static Analysis", 25, "ANALYZING");
            Review review = staticAnalysisService.analyze(submission, sourceCode);
            updateProgress(submission, "Static Analysis Complete", 40, "ANALYZING");

            updateProgress(submission, "Metrics Generation", 50, "ANALYZING");
            metricsService.calculateMetrics(submission, sourceCode);
            updateProgress(submission, "Metrics Complete", 60, "ANALYZING");

            updateProgress(submission, "AI Review", 65, "AI_REVIEWING");
            aiReviewService.reviewCode(submission, review, sourceCode);
            updateProgress(submission, "AI Review Complete", 80, "AI_REVIEWING");

            updateProgress(submission, "Plagiarism Scan", 85, "PLAGIARISM_CHECKING");
            plagiarismService.checkPlagiarism(submission, sourceCode);
            updateProgress(submission, "Plagiarism Complete", 95, "PLAGIARISM_CHECKING");

            updateProgress(submission, "Completed", 100, "COMPLETED");
            submission.setStatus(Submission.SubmissionStatus.COMPLETED);
            submission.setCompletedAt(java.time.LocalDateTime.now());
            submissionRepository.save(submission);

        } catch (Exception e) {
            log.error("Processing failed for submission {}: {}", submissionId, e.getMessage(), e);
            Submission submission = submissionRepository.findById(Objects.requireNonNull(submissionId)).orElse(null);
            if (submission != null) {
                submission.setStatus(Submission.SubmissionStatus.FAILED);
                submission.setCurrentStage("Failed: " + e.getMessage());
                submissionRepository.save(submission);
                sendProgress(submission.getId(), "Failed", 0, e.getMessage(), "FAILED");
            }
        }
    }

    private void updateProgress(Submission sub, String stage, int percent, String status) {
        sub.setCurrentStage(stage);
        sub.setProgressPercent(percent);
        sub.setStatus(Submission.SubmissionStatus.valueOf(status));
        submissionRepository.save(sub);
        sendProgress(sub.getId(), stage, percent, stage, status);
    }

    private void sendProgress(Long submissionId, String stage, int percent, String msg, String status) {
        ReviewDTO.ProgressUpdate update = ReviewDTO.ProgressUpdate.builder()
                .submissionId(submissionId).stage(stage)
                .progressPercent(percent).message(msg).status(status).build();
        messagingTemplate.convertAndSend("/topic/progress/" + submissionId, Objects.requireNonNull(update));
    }

    public ReviewDTO.SubmissionResponse getSubmission(Long submissionId, Long userId) {
        Submission s = submissionRepository.findById(Objects.requireNonNull(submissionId))
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", submissionId));
        if (!s.getUser().getId().equals(userId))
            throw new BadRequestException("No permission");
        return mapToResponse(s);
    }

    public Page<ReviewDTO.SubmissionResponse> getProjectSubmissions(Long projectId, Long userId, Pageable pageable) {
        Project project = projectRepository.findById(Objects.requireNonNull(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        if (!project.getOwner().getId().equals(userId) && !project.getIsPublic())
            throw new BadRequestException("No permission");
        return submissionRepository.findByProjectId(projectId, pageable).map(this::mapToResponse);
    }

    public Page<ReviewDTO.SubmissionResponse> getUserSubmissions(Long userId, Pageable pageable) {
        return submissionRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    public String getSourceCode(Long submissionId, Long userId) {
        Submission s = submissionRepository.findById(Objects.requireNonNull(submissionId))
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", submissionId));
        if (!s.getUser().getId().equals(userId))
            throw new BadRequestException("No permission");
        return s.getSourceCode();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new BadRequestException("File is empty");
        String ext = getExtension(file.getOriginalFilename());
        if (!Arrays.asList(allowedExtensions.split(",")).contains(ext))
            throw new BadRequestException("File type not allowed: " + ext);
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : "";
    }

    private String detectLanguage(String ext) {
        return switch (ext) {
            case ".java" -> "Java";
            case ".py" -> "Python";
            case ".js" -> "JavaScript";
            case ".ts" -> "TypeScript";
            case ".cpp", ".hpp" -> "C++";
            case ".c", ".h" -> "C";
            case ".go" -> "Go";
            case ".rs" -> "Rust";
            case ".rb" -> "Ruby";
            case ".php" -> "PHP";
            case ".swift" -> "Swift";
            case ".kt" -> "Kotlin";
            default -> "Other";
        };
    }

    private ReviewDTO.SubmissionResponse mapToResponse(Submission s) {
        ReviewDTO.SubmissionResponse.SubmissionResponseBuilder builder = ReviewDTO.SubmissionResponse.builder()
                .id(s.getId()).fileName(s.getFileName()).originalFileName(s.getOriginalFileName())
                .language(s.getLanguage()).fileSize(s.getFileSize())
                .status(s.getStatus().name()).currentStage(s.getCurrentStage())
                .progressPercent(s.getProgressPercent()).projectId(s.getProject().getId())
                .projectName(s.getProject().getName())
                .createdAt(s.getCreatedAt()).completedAt(s.getCompletedAt());

        if (s.getReview() != null) {
            Review r = s.getReview();
            List<ReviewDTO.FindingResponse> findings = r.getFindings().stream()
                    .map(f -> ReviewDTO.FindingResponse.builder()
                            .id(f.getId()).ruleId(f.getRuleId()).title(f.getTitle())
                            .description(f.getDescription()).severity(f.getSeverity().name())
                            .lineNumber(f.getLineNumber()).endLineNumber(f.getEndLineNumber())
                            .codeSnippet(f.getCodeSnippet()).recommendation(f.getRecommendation())
                            .fixedCode(f.getFixedCode()).source(f.getSource().name()).build())
                    .collect(Collectors.toList());
            builder.review(ReviewDTO.ReviewSummary.builder()
                    .id(r.getId()).totalIssues(r.getTotalIssues())
                    .criticalCount(r.getCriticalCount()).warningCount(r.getWarningCount())
                    .infoCount(r.getInfoCount()).overallScore(r.getOverallScore())
                    .summary(r.getSummary()).findings(findings).build());
        }
        if (s.getMetrics() != null) {
            CodeMetrics m = s.getMetrics();
            builder.metrics(ReviewDTO.MetricsSummary.builder()
                    .linesOfCode(m.getLinesOfCode()).blankLines(m.getBlankLines())
                    .commentLines(m.getCommentLines()).codeLines(m.getCodeLines())
                    .commentRatio(m.getCommentRatio()).cyclomaticComplexity(m.getCyclomaticComplexity())
                    .maintainabilityIndex(m.getMaintainabilityIndex())
                    .numberOfMethods(m.getNumberOfMethods()).numberOfClasses(m.getNumberOfClasses())
                    .averageMethodLength(m.getAverageMethodLength())
                    .maxNestingDepth(m.getMaxNestingDepth()).numberOfImports(m.getNumberOfImports()).build());
        }
        if (s.getPlagiarismResult() != null) {
            PlagiarismResult p = s.getPlagiarismResult();
            builder.plagiarism(ReviewDTO.PlagiarismSummary.builder()
                    .similarityPercentage(p.getSimilarityPercentage())
                    .matchingSections(p.getMatchingSections())
                    .comparedSubmissionId(p.getComparedSubmissionId())
                    .comparedFileName(p.getComparedFileName())
                    .flagged(p.getFlagged()).build());
        }
        return builder.build();
    }
}
