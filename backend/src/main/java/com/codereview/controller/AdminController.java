package com.codereview.controller;

import com.codereview.dto.AnalyticsDTO;
import com.codereview.dto.ApiResponse;
import com.codereview.entity.AuditLog;
import com.codereview.entity.User;
import com.codereview.exception.ResourceNotFoundException;
import com.codereview.repository.UserRepository;
import com.codereview.service.AnalyticsService;
import com.codereview.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;
    private final AuditService auditService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AnalyticsDTO.AdminStats>> stats() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getAdminStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<User>>> users(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) @NonNull Pageable pageable) {
        Page<User> users = (search != null && !search.isBlank())
                ? userRepository.searchUsers(search, pageable)
                : userRepository.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/users/{id}/toggle")
    public ResponseEntity<ApiResponse<String>> toggleUser(@PathVariable @NonNull Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(user.getEnabled() ? "User enabled" : "User disabled", null));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> auditLogs(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getAuditLogs(pageable)));
    }
}
