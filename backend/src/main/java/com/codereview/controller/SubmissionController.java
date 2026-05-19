package com.codereview.controller;

import com.codereview.dto.ApiResponse;
import com.codereview.dto.ReviewDTO;
import com.codereview.security.CustomUserDetails;
import com.codereview.service.ReportService;
import com.codereview.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final ReportService reportService;

    @PostMapping("/upload/{projectId}")
    public ResponseEntity<ApiResponse<ReviewDTO.SubmissionResponse>> upload(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("File uploaded, processing started",
                submissionService.uploadAndProcess(user.getId(), projectId, file)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewDTO.SubmissionResponse>> get(
            @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(submissionService.getSubmission(id, user.getId())));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<Page<ReviewDTO.SubmissionResponse>>> byProject(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long projectId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                submissionService.getProjectSubmissions(projectId, user.getId(), pageable)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<ReviewDTO.SubmissionResponse>>> my(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(submissionService.getUserSubmissions(user.getId(), pageable)));
    }

    @GetMapping("/{id}/source")
    public ResponseEntity<ApiResponse<String>> source(
            @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(submissionService.getSourceCode(id, user.getId())));
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> report(
            @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        byte[] pdf = reportService.generateReport(id, user.getId());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("review-report-" + id + ".pdf").build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
