package com.codereview.service;

import com.codereview.entity.CodeMetrics;
import com.codereview.entity.Submission;
import com.codereview.repository.MetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MetricsRepository metricsRepository;

    @Transactional
    public CodeMetrics calculateMetrics(Submission submission, String sourceCode) {
        String[] lines = sourceCode.split("\n");
        int totalLines = lines.length;
        int blankLines = 0, commentLines = 0, codeLines = 0;
        int cyclomaticComplexity = 1;
        int maxNesting = 0, currentNesting = 0;
        int methodCount = 0, classCount = 0;
        int importCount = 0;
        boolean inBlockComment = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                blankLines++;
                continue;
            }

            if (inBlockComment) {
                commentLines++;
                if (trimmed.contains("*/")) inBlockComment = false;
                continue;
            }

            if (trimmed.startsWith("/*")) {
                commentLines++;
                if (!trimmed.contains("*/")) inBlockComment = true;
                continue;
            }

            if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
                commentLines++;
                continue;
            }

            codeLines++;

            // Cyclomatic complexity
            if (trimmed.matches(".*\\b(if|else if|elif|case|for|while|catch|&&|\\|\\|)\\b.*")) {
                cyclomaticComplexity++;
            }
            if (trimmed.contains("?") && trimmed.contains(":")) {
                cyclomaticComplexity++;
            }

            // Nesting
            for (char c : trimmed.toCharArray()) {
                if (c == '{' || c == '(') currentNesting++;
                if (c == '}' || c == ')') currentNesting--;
            }
            maxNesting = Math.max(maxNesting, currentNesting);

            // Method/function detection
            if (trimmed.matches(".*\\b(public|private|protected|static|def|function|func|fn)\\b.*\\(.*\\).*")) {
                methodCount++;
            }

            // Class detection
            if (trimmed.matches(".*\\b(class|interface|enum|struct)\\s+\\w+.*")) {
                classCount++;
            }

            // Import detection
            if (trimmed.startsWith("import ") || trimmed.startsWith("from ") ||
                    trimmed.startsWith("require(") || trimmed.startsWith("use ") ||
                    trimmed.startsWith("#include")) {
                importCount++;
            }
        }

        double commentRatio = codeLines > 0 ? (double) commentLines / codeLines * 100.0 : 0;
        double avgMethodLength = methodCount > 0 ? (double) codeLines / methodCount : codeLines;

        // Maintainability Index (simplified SEI formula)
        double halsteadVolume = codeLines * Math.log(Math.max(1, codeLines)) / Math.log(2);
        double maintainability = Math.max(0, 171.0 - 5.2 * Math.log(halsteadVolume)
                - 0.23 * cyclomaticComplexity - 16.2 * Math.log(Math.max(1, codeLines)));
        maintainability = Math.min(100, maintainability * 100.0 / 171.0);

        CodeMetrics metrics = CodeMetrics.builder()
                .submission(submission)
                .linesOfCode(totalLines).blankLines(blankLines)
                .commentLines(commentLines).codeLines(codeLines)
                .commentRatio(Math.round(commentRatio * 10.0) / 10.0)
                .cyclomaticComplexity(cyclomaticComplexity)
                .maintainabilityIndex(Math.round(maintainability * 10.0) / 10.0)
                .numberOfMethods(methodCount).numberOfClasses(classCount)
                .averageMethodLength(Math.round(avgMethodLength * 10.0) / 10.0)
                .maxNestingDepth(maxNesting).numberOfImports(importCount)
                .build();

        return metricsRepository.save(Objects.requireNonNull(metrics));
    }
}
