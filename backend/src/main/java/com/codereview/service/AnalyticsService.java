package com.codereview.service;

import com.codereview.dto.AnalyticsDTO;
import com.codereview.entity.Finding;
import com.codereview.entity.Submission;
import com.codereview.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final SubmissionRepository submissionRepository;
    private final ProjectRepository projectRepository;
    private final ReviewRepository reviewRepository;
    private final FindingRepository findingRepository;
    private final MetricsRepository metricsRepository;
    private final UserRepository userRepository;

    public AnalyticsDTO.DashboardStats getDashboardStats(Long userId) {
        long totalSubs = submissionRepository.countByUserId(userId);
        long totalProjects = projectRepository.countByOwnerId(userId);
        Long totalIssues = reviewRepository.totalIssuesByUserId(userId);
        Double avgScore = reviewRepository.averageScoreByUserId(userId);
        Double avgMaint = metricsRepository.averageMaintainabilityByUserId(userId);

        Map<String, Long> langDist = new LinkedHashMap<>();
        submissionRepository.countByLanguageForUser(userId).forEach(row ->
                langDist.put((String) row[0], (Long) row[1]));

        Map<String, Long> sevDist = new LinkedHashMap<>();
        findingRepository.countBySeverityForUser(userId).forEach(row ->
                sevDist.put(((Finding.Severity) row[0]).name(), (Long) row[1]));

        List<Submission> recent = submissionRepository.findRecentByUserId(userId, PageRequest.of(0, 10));
        List<AnalyticsDTO.RecentActivity> activities = recent.stream().map(s ->
                AnalyticsDTO.RecentActivity.builder()
                        .submissionId(s.getId())
                        .fileName(s.getOriginalFileName())
                        .projectName(s.getProject().getName())
                        .status(s.getStatus().name())
                        .score(s.getReview() != null ? s.getReview().getOverallScore() : null)
                        .createdAt(s.getCreatedAt().toString())
                        .build()).collect(Collectors.toList());

        return AnalyticsDTO.DashboardStats.builder()
                .totalSubmissions(totalSubs)
                .totalProjects(totalProjects)
                .totalIssuesFound(totalIssues != null ? totalIssues : 0)
                .averageScore(avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : 0)
                .averageMaintainability(avgMaint != null ? Math.round(avgMaint * 10.0) / 10.0 : 0)
                .languageDistribution(langDist)
                .severityDistribution(sevDist)
                .recentActivities(activities)
                .build();
    }

    public AnalyticsDTO.AdminStats getAdminStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabled(true);
        long totalSubs = submissionRepository.count();
        long subsToday = submissionRepository.countSince(LocalDateTime.now().minusDays(1));
        long totalProjects = projectRepository.count();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Submission.SubmissionStatus status : Submission.SubmissionStatus.values()) {
            byStatus.put(status.name(), submissionRepository.countByStatus(status));
        }

        return AnalyticsDTO.AdminStats.builder()
                .totalUsers(totalUsers).activeUsers(activeUsers)
                .totalSubmissions(totalSubs).submissionsToday(subsToday)
                .totalProjects(totalProjects).submissionsByStatus(byStatus)
                .build();
    }
}
