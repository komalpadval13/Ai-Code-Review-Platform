package com.codereview.controller;

import com.codereview.dto.ApiResponse;
import com.codereview.dto.ProjectDTO;
import com.codereview.security.CustomUserDetails;
import com.codereview.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectDTO.Response>> create(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ProjectDTO.CreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Project created", projectService.createProject(user.getId(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDTO.Response>> update(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody ProjectDTO.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(projectService.updateProject(user.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        projectService.deleteProject(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Project deleted", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDTO.Response>> get(
            @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProject(id, user.getId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProjectDTO.Response>>> list(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getUserProjects(user.getId(), search, pageable)));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Page<ProjectDTO.Response>>> publicProjects(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getPublicProjects(pageable)));
    }
}
