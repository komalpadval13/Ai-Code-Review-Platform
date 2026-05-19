package com.codereview.service;

import com.codereview.entity.*;
import com.codereview.repository.FindingRepository;
import com.codereview.repository.ReviewRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIReviewService {

    private final ReviewRepository reviewRepository;
    private final FindingRepository findingRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.api-url}")
    private String apiUrl;

    @Value("${app.ai.model}")
    private String model;

    @Value("${app.ai.max-tokens}")
    private int maxTokens;

    @Value("${app.ai.temperature}")
    private double temperature;

    @Value("${app.ai.provider}")
    private String provider;

    public void reviewCode(Submission submission, Review review, String sourceCode) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI API key not configured, skipping AI review");
            return;
        }

        try {
            String prompt = buildPrompt(sourceCode, submission.getLanguage());
            String response = callAIProvider(prompt);
            parseAndSaveFindings(response, review);
        } catch (Exception e) {
            log.error("AI review failed: {}", e.getMessage());
        }
    }

    private String buildPrompt(String sourceCode, String language) {
        return """
            You are an expert code reviewer. Analyze the following %s code and return a JSON array of issues found.
            
            Each issue must have this exact structure:
            {
              "ruleId": "AI_<RULE_NAME>",
              "title": "Short title",
              "description": "Detailed explanation of the issue",
              "severity": "CRITICAL" | "WARNING" | "INFO",
              "lineNumber": <line number>,
              "recommendation": "How to fix it",
              "fixedCode": "corrected code snippet (optional, can be null)"
            }
            
            Focus on:
            - Bad practices and anti-patterns
            - Performance issues
            - Security vulnerabilities
            - Code readability problems
            - Missing error handling
            - Potential bugs
            - Code optimization opportunities
            
            Return ONLY a valid JSON array. No markdown, no explanation, just the JSON array.
            If no issues found, return an empty array [].
            
            Code to review:
            ```%s
            %s
            ```
            """.formatted(language, language.toLowerCase(), sourceCode);
    }

    @SuppressWarnings("unchecked")
    private String callAIProvider(String prompt) {
        WebClient client = WebClient.builder().baseUrl(Objects.requireNonNull(apiUrl)).build();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "You are an expert code reviewer. Always respond with valid JSON."),
                Map.of("role", "user", "content", prompt)
        );
        requestBody.put("messages", messages);

        String response = client.post()
                .header("Authorization", "Bearer " + apiKey)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            Map<String, Object> parsed = objectMapper.readValue(response, new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
        }
        return "[]";
    }

    private void parseAndSaveFindings(String jsonResponse, Review review) {
        try {
            String cleaned = jsonResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
            }

            List<Map<String, Object>> issues = objectMapper.readValue(cleaned, new TypeReference<>() {});
            List<Finding> findings = new ArrayList<>();

            for (Map<String, Object> issue : issues) {
                Finding finding = Finding.builder()
                        .review(review)
                        .ruleId((String) issue.getOrDefault("ruleId", "AI_REVIEW"))
                        .title((String) issue.getOrDefault("title", "AI Finding"))
                        .description((String) issue.getOrDefault("description", ""))
                        .severity(Finding.Severity.valueOf(
                                ((String) issue.getOrDefault("severity", "INFO")).toUpperCase()))
                        .lineNumber(issue.get("lineNumber") != null ?
                                ((Number) issue.get("lineNumber")).intValue() : null)
                        .recommendation((String) issue.get("recommendation"))
                        .fixedCode((String) issue.get("fixedCode"))
                        .source(Finding.FindingSource.AI)
                        .build();
                findings.add(finding);
            }

            if (!findings.isEmpty()) {
                findingRepository.saveAll(findings);
                review.getFindings().addAll(findings);
                int crit = 0, warn = 0, inf = 0;
                for (Finding f : findings) {
                    switch (f.getSeverity()) {
                        case CRITICAL -> crit++;
                        case WARNING -> warn++;
                        case INFO -> inf++;
                    }
                }
                review.setTotalIssues(review.getTotalIssues() + findings.size());
                review.setCriticalCount(review.getCriticalCount() + crit);
                review.setWarningCount(review.getWarningCount() + warn);
                review.setInfoCount(review.getInfoCount() + inf);
                review.setSource(Review.ReviewSource.COMBINED);
                reviewRepository.save(review);
            }
        } catch (Exception e) {
            log.error("Failed to parse AI findings: {}", e.getMessage());
        }
    }
}
