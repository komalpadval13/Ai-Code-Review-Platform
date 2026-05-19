package com.codereview.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

public class AnalyticsDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DashboardStats {
        private Long totalSubmissions;
        private Long totalProjects;
        private Long totalIssuesFound;
        private Double averageScore;
        private Double averageMaintainability;
        private Map<String, Long> languageDistribution;
        private Map<String, Long> severityDistribution;
        private List<RecentActivity> recentActivities;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecentActivity {
        private Long submissionId;
        private String fileName;
        private String projectName;
        private String status;
        private Double score;
        private String createdAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AdminStats {
        private Long totalUsers;
        private Long activeUsers;
        private Long totalSubmissions;
        private Long submissionsToday;
        private Long totalProjects;
        private Map<String, Long> submissionsByStatus;
    }
}
