package com.codereview.service;

import com.codereview.entity.Review;
import com.codereview.entity.Submission;
import com.codereview.entity.Finding;
import com.codereview.entity.CodeMetrics;
import com.codereview.entity.PlagiarismResult;
import com.codereview.exception.ResourceNotFoundException;
import com.codereview.repository.SubmissionRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final SubmissionRepository submissionRepository;

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, new BaseColor(45, 55, 72));
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(74, 85, 104));
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);
    private static final BaseColor TABLE_HEADER_BG = new BaseColor(99, 102, 241);

    public byte[] generateReport(Long submissionId, Long userId) {
        Submission sub = submissionRepository.findById(Objects.requireNonNull(submissionId))
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", submissionId));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Title
            Paragraph title = new Paragraph("AI Code Review Report", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph("File: " + sub.getOriginalFileName(), NORMAL_FONT));
            doc.add(new Paragraph("Language: " + sub.getLanguage(), NORMAL_FONT));
            doc.add(new Paragraph("Generated: " + java.time.LocalDateTime.now(), SMALL_FONT));
            doc.add(Chunk.NEWLINE);

            // Review Summary
            if (sub.getReview() != null) {
                Review r = sub.getReview();
                doc.add(new Paragraph("Review Summary", HEADER_FONT));
                doc.add(new Paragraph("Overall Score: " + r.getOverallScore() + "/100", NORMAL_FONT));
                doc.add(new Paragraph("Total Issues: " + r.getTotalIssues(), NORMAL_FONT));
                doc.add(new Paragraph("Critical: " + r.getCriticalCount() + " | Warnings: " +
                        r.getWarningCount() + " | Info: " + r.getInfoCount(), NORMAL_FONT));
                doc.add(Chunk.NEWLINE);

                // Findings table
                if (!r.getFindings().isEmpty()) {
                    doc.add(new Paragraph("Findings", HEADER_FONT));
                    PdfPTable table = new PdfPTable(5);
                    table.setWidthPercentage(100);
                    table.setWidths(new float[]{1, 2, 3, 1.5f, 3});
                    addTableHeader(table, "Line", "Rule", "Description", "Severity", "Recommendation");

                    for (Finding f : r.getFindings()) {
                        table.addCell(cellNormal(f.getLineNumber() != null ? String.valueOf(f.getLineNumber()) : "-"));
                        table.addCell(cellNormal(f.getRuleId()));
                        table.addCell(cellNormal(truncate(f.getDescription(), 80)));
                        table.addCell(cellSeverity(f.getSeverity().name()));
                        table.addCell(cellNormal(truncate(f.getRecommendation(), 80)));
                    }
                    doc.add(table);
                    doc.add(Chunk.NEWLINE);
                }
            }

            // Metrics
            if (sub.getMetrics() != null) {
                CodeMetrics m = sub.getMetrics();
                doc.add(new Paragraph("Code Metrics", HEADER_FONT));
                PdfPTable mt = new PdfPTable(2);
                mt.setWidthPercentage(60);
                addMetricRow(mt, "Lines of Code", String.valueOf(m.getLinesOfCode()));
                addMetricRow(mt, "Code Lines", String.valueOf(m.getCodeLines()));
                addMetricRow(mt, "Comment Lines", String.valueOf(m.getCommentLines()));
                addMetricRow(mt, "Comment Ratio", m.getCommentRatio() + "%");
                addMetricRow(mt, "Cyclomatic Complexity", String.valueOf(m.getCyclomaticComplexity()));
                addMetricRow(mt, "Maintainability Index", String.valueOf(m.getMaintainabilityIndex()));
                addMetricRow(mt, "Methods", String.valueOf(m.getNumberOfMethods()));
                addMetricRow(mt, "Classes", String.valueOf(m.getNumberOfClasses()));
                addMetricRow(mt, "Max Nesting", String.valueOf(m.getMaxNestingDepth()));
                doc.add(mt);
                doc.add(Chunk.NEWLINE);
            }

            // Plagiarism
            if (sub.getPlagiarismResult() != null) {
                PlagiarismResult p = sub.getPlagiarismResult();
                doc.add(new Paragraph("Plagiarism Detection", HEADER_FONT));
                doc.add(new Paragraph("Similarity: " + p.getSimilarityPercentage() + "%", NORMAL_FONT));
                doc.add(new Paragraph("Flagged: " + (p.getFlagged() ? "YES" : "No"), NORMAL_FONT));
                if (p.getComparedFileName() != null) {
                    doc.add(new Paragraph("Compared with: " + p.getComparedFileName(), NORMAL_FONT));
                }
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE)));
            cell.setBackgroundColor(TABLE_HEADER_BG);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private PdfPCell cellNormal(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", SMALL_FONT));
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell cellSeverity(String severity) {
        BaseColor color = switch (severity) {
            case "CRITICAL" -> new BaseColor(220, 38, 38);
            case "WARNING" -> new BaseColor(234, 179, 8);
            default -> new BaseColor(59, 130, 246);
        };
        Font f = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, color);
        PdfPCell cell = new PdfPCell(new Phrase(severity, f));
        cell.setPadding(4);
        return cell;
    }

    private void addMetricRow(PdfPTable table, String label, String value) {
        table.addCell(cellNormal(label));
        PdfPCell cell = new PdfPCell(new Phrase(value, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String truncate(String s, int max) {
        if (s == null) return "-";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
