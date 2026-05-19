package com.codereview.controller;

import com.codereview.dto.AnalyticsDTO;
import com.codereview.dto.ApiResponse;
import com.codereview.security.CustomUserDetails;
import com.codereview.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AnalyticsDTO.DashboardStats>> dashboard(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDashboardStats(user.getId())));
    }
}
