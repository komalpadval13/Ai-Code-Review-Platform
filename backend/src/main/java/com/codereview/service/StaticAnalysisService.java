package com.codereview.service;

import com.codereview.entity.*;
import com.codereview.repository.FindingRepository;
import com.codereview.repository.ReviewRepository;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaticAnalysisService {

    private final ReviewRepository reviewRepository;
    private final FindingRepository findingRepository;

    @Transactional
    public Review analyze(Submission submission, String sourceCode) {
        Review review = Review.builder()
                .submission(submission).source(Review.ReviewSource.STATIC).build();
        review = reviewRepository.save(Objects.requireNonNull(review));

        List<Finding> findings = new ArrayList<>();

        if ("Java".equals(submission.getLanguage())) {
            findings.addAll(analyzeJava(review, sourceCode));
        } else {
            findings.addAll(analyzeGeneric(review, sourceCode, submission.getLanguage()));
        }

        int critical = 0, warning = 0, info = 0;
        for (Finding f : findings) {
            switch (f.getSeverity()) {
                case CRITICAL -> critical++;
                case WARNING -> warning++;
                case INFO -> info++;
            }
        }

        review.setTotalIssues(findings.size());
        review.setCriticalCount(critical);
        review.setWarningCount(warning);
        review.setInfoCount(info);

        double score = Math.max(0, 100.0 - (critical * 15) - (warning * 5) - (info * 1));
        review.setOverallScore(Math.round(score * 10.0) / 10.0);
        review.setSummary(String.format("Found %d issues: %d critical, %d warnings, %d info",
                findings.size(), critical, warning, info));
        review.setFindings(findings);
        return reviewRepository.save(review);
    }

    private List<Finding> analyzeJava(Review review, String sourceCode) {
        List<Finding> findings = new ArrayList<>();
        try {
            JavaParser parser = new JavaParser();
            var result = parser.parse(sourceCode);
            if (result.getResult().isEmpty()) {
                findings.add(createFinding(review, "PARSE_ERROR", "Parse Error",
                        "Failed to parse Java source code", Finding.Severity.CRITICAL, 1,
                        "Ensure valid Java syntax", null));
                return findings;
            }
            CompilationUnit cu = result.getResult().get();

            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodDeclaration md, Void arg) {
                    super.visit(md, arg);
                    int startLine = md.getBegin().map(p -> p.line).orElse(0);

                    // Long method detection
                    if (md.getBody().isPresent()) {
                        long lines = md.getEnd().map(p -> p.line).orElse(0) - startLine;
                        if (lines > 50) {
                            findings.add(createFinding(review, "LONG_METHOD", "Long Method",
                                    "Method '" + md.getNameAsString() + "' has " + lines + " lines. Methods should be under 50 lines.",
                                    Finding.Severity.WARNING, startLine,
                                    "Break this method into smaller, focused methods", null));
                        }
                    }

                    // Empty method body
                    if (md.getBody().isPresent() && md.getBody().get().getStatements().isEmpty()
                            && !md.getNameAsString().equals("main")) {
                        findings.add(createFinding(review, "EMPTY_METHOD", "Empty Method Body",
                                "Method '" + md.getNameAsString() + "' has an empty body",
                                Finding.Severity.INFO, startLine,
                                "Add implementation or document why the method is empty", null));
                    }
                }

                @Override
                public void visit(CatchClause cc, Void arg) {
                    super.visit(cc, arg);
                    if (cc.getBody().getStatements().isEmpty()) {
                        int line = cc.getBegin().map(p -> p.line).orElse(0);
                        findings.add(createFinding(review, "EMPTY_CATCH", "Empty Catch Block",
                                "Empty catch block swallows exceptions silently",
                                Finding.Severity.CRITICAL, line,
                                "Log the exception or handle it properly",
                                "catch (" + cc.getParameter().getTypeAsString() + " e) {\n    log.error(\"Error: \", e);\n}"));
                    }
                }

                @Override
                public void visit(IntegerLiteralExpr ile, Void arg) {
                    super.visit(ile, arg);
                    int val = ile.asNumber().intValue();
                    if (val != 0 && val != 1 && val != -1 && val != 2) {
                        int line = ile.getBegin().map(p -> p.line).orElse(0);
                        findings.add(createFinding(review, "MAGIC_NUMBER", "Magic Number",
                                "Magic number " + val + " should be replaced with a named constant",
                                Finding.Severity.INFO, line,
                                "Extract to a constant with a descriptive name", null));
                    }
                }

                @Override
                public void visit(DoubleLiteralExpr dle, Void arg) {
                    super.visit(dle, arg);
                    double val = dle.asDouble();
                    if (val != 0.0 && val != 1.0) {
                        int line = dle.getBegin().map(p -> p.line).orElse(0);
                        findings.add(createFinding(review, "MAGIC_NUMBER", "Magic Number",
                                "Magic number " + val + " should be replaced with a named constant",
                                Finding.Severity.INFO, line,
                                "Extract to a constant with a descriptive name", null));
                    }
                }
            }, null);

            // Deep nesting detection
            checkNesting(cu, review, findings);

            // Security risk: System.exit
            String[] lines = sourceCode.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.contains("System.exit")) {
                    findings.add(createFinding(review, "SECURITY_SYSTEM_EXIT", "System.exit() Call",
                            "System.exit() can terminate the JVM abruptly",
                            Finding.Severity.CRITICAL, i + 1,
                            "Use proper exception handling instead", null));
                }
                if (line.contains("Runtime.getRuntime().exec")) {
                    findings.add(createFinding(review, "SECURITY_EXEC", "Runtime.exec() Call",
                            "Runtime.exec() can be a security vulnerability",
                            Finding.Severity.CRITICAL, i + 1,
                            "Validate and sanitize all input before execution", null));
                }
                if (line.contains("// TODO") || line.contains("// FIXME") || line.contains("// HACK")) {
                    findings.add(createFinding(review, "TODO_COMMENT", "TODO/FIXME Comment",
                            "Found unresolved TODO/FIXME comment",
                            Finding.Severity.INFO, i + 1,
                            "Resolve or create a tracking issue", null));
                }
            }
        } catch (Exception e) {
            log.warn("Java analysis partially failed: {}", e.getMessage());
            findings.addAll(analyzeGeneric(review, sourceCode, "Java"));
        }
        findingRepository.saveAll(findings);
        return findings;
    }

    private void checkNesting(CompilationUnit cu, Review review, List<Finding> findings) {
        cu.accept(new VoidVisitorAdapter<Integer>() {
            @Override
            public void visit(IfStmt n, Integer depth) {
                if (depth > 3) {
                    int line = n.getBegin().map(p -> p.line).orElse(0);
                    findings.add(createFinding(review, "DEEP_NESTING", "Deep Nesting",
                            "Code is nested " + depth + " levels deep",
                            Finding.Severity.WARNING, line,
                            "Refactor using early returns or extract methods", null));
                }
                super.visit(n, depth + 1);
            }
            @Override
            public void visit(ForStmt n, Integer depth) {
                if (depth > 3) {
                    int line = n.getBegin().map(p -> p.line).orElse(0);
                    findings.add(createFinding(review, "DEEP_NESTING", "Deep Nesting",
                            "Loop is nested " + depth + " levels deep",
                            Finding.Severity.WARNING, line,
                            "Extract inner logic to separate methods", null));
                }
                super.visit(n, depth + 1);
            }
            @Override
            public void visit(WhileStmt n, Integer depth) {
                if (depth > 3) {
                    int line = n.getBegin().map(p -> p.line).orElse(0);
                    findings.add(createFinding(review, "DEEP_NESTING", "Deep Nesting",
                            "Loop is nested " + depth + " levels deep",
                            Finding.Severity.WARNING, line,
                            "Extract inner logic to separate methods", null));
                }
                super.visit(n, depth + 1);
            }
        }, 0);
    }

    private List<Finding> analyzeGeneric(Review review, String sourceCode, String language) {
        List<Finding> findings = new ArrayList<>();
        String[] lines = sourceCode.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // Long lines
            if (line.length() > 120) {
                findings.add(createFinding(review, "LONG_LINE", "Long Line",
                        "Line exceeds 120 characters (" + line.length() + ")",
                        Finding.Severity.INFO, i + 1,
                        "Break into multiple lines for readability", null));
            }
            // Unresolved comment markers
            if (trimmed.contains("TODO") || trimmed.contains("FIXME") || trimmed.contains("HACK")) {
                findings.add(createFinding(review, "TODO_COMMENT", "TODO/FIXME Comment",
                        "Found unresolved comment marker", Finding.Severity.INFO, i + 1,
                        "Resolve or create a tracking issue", null));
            }
            // Hardcoded credentials
            if (trimmed.matches(".*(?i)(password|secret|api_key|apikey|token)\\s*=\\s*[\"'].+[\"'].*")) {
                findings.add(createFinding(review, "HARDCODED_SECRET", "Hardcoded Secret",
                        "Possible hardcoded credential detected",
                        Finding.Severity.CRITICAL, i + 1,
                        "Use environment variables or a secrets manager", null));
            }
            // Consecutive blank lines
            if (i > 0 && trimmed.isEmpty() && lines[i - 1].trim().isEmpty() && i > 1 && lines[i - 2].trim().isEmpty()) {
                findings.add(createFinding(review, "EXCESSIVE_BLANKS", "Excessive Blank Lines",
                        "More than 2 consecutive blank lines",
                        Finding.Severity.INFO, i + 1,
                        "Reduce to a single blank line", null));
            }
        }

        // Check total length
        if (lines.length > 500) {
            findings.add(createFinding(review, "LARGE_FILE", "Large File",
                    "File has " + lines.length + " lines", Finding.Severity.WARNING, 1,
                    "Consider splitting into smaller modules", null));
        }

        findingRepository.saveAll(findings);
        return findings;
    }

    private Finding createFinding(Review review, String ruleId, String title, String desc,
                                   Finding.Severity severity, int line, String recommendation, String fixedCode) {
        return Finding.builder().review(review).ruleId(ruleId).title(title)
                .description(desc).severity(severity).lineNumber(line)
                .recommendation(recommendation).fixedCode(fixedCode)
                .source(Finding.FindingSource.STATIC).build();
    }
}
