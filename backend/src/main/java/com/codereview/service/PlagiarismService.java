package com.codereview.service;

import com.codereview.entity.PlagiarismResult;
import com.codereview.entity.Submission;
import com.codereview.repository.PlagiarismRepository;
import com.codereview.repository.SubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlagiarismService {

    private final PlagiarismRepository plagiarismRepository;
    private final SubmissionRepository submissionRepository;
    private final ObjectMapper objectMapper;

    private static final int K_GRAM_SIZE = 5;
    private static final int WINDOW_SIZE = 4;
    private static final double FLAGGING_THRESHOLD = 40.0;

    @Transactional
    public PlagiarismResult checkPlagiarism(Submission submission, String sourceCode) {
        String normalized = normalize(sourceCode);
        List<Long> fingerprints = winnow(normalized);

        List<Submission> others = submissionRepository.findSimilarSubmissions(
                submission.getProject().getId(), submission.getLanguage(), submission.getId());

        double maxSimilarity = 0;
        Long matchedId = null;
        String matchedFile = null;
        List<Map<String, Object>> matchingSections = new ArrayList<>();

        for (Submission other : others) {
            if (other.getSourceCode() == null) continue;
            String otherNorm = normalize(other.getSourceCode());
            List<Long> otherFingerprints = winnow(otherNorm);

            double similarity = jaccardSimilarity(fingerprints, otherFingerprints);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                matchedId = other.getId();
                matchedFile = other.getOriginalFileName();
                matchingSections = findMatchingSections(sourceCode, other.getSourceCode(), normalized, otherNorm);
            }
        }

        String fpJson;
        String matchJson;
        try {
            fpJson = objectMapper.writeValueAsString(fingerprints.stream().limit(100).collect(Collectors.toList()));
            matchJson = objectMapper.writeValueAsString(matchingSections);
        } catch (Exception e) {
            fpJson = "[]";
            matchJson = "[]";
        }

        PlagiarismResult result = PlagiarismResult.builder()
                .submission(submission)
                .similarityPercentage(Math.round(maxSimilarity * 1000.0) / 10.0)
                .fingerprints(fpJson)
                .matchingSections(matchJson)
                .comparedSubmissionId(matchedId)
                .comparedFileName(matchedFile)
                .flagged(maxSimilarity * 100 >= FLAGGING_THRESHOLD)
                .build();

        return plagiarismRepository.save(Objects.requireNonNull(result));
    }

    private String normalize(String code) {
        return code.replaceAll("//.*", "")
                .replaceAll("/\\*[\\s\\S]*?\\*/", "")
                .replaceAll("#.*", "")
                .replaceAll("\\s+", " ")
                .replaceAll("[\"'].*?[\"']", "S")
                .toLowerCase().trim();
    }

    private List<Long> kGrams(String text) {
        List<Long> hashes = new ArrayList<>();
        for (int i = 0; i <= text.length() - K_GRAM_SIZE; i++) {
            hashes.add(hashKGram(text.substring(i, i + K_GRAM_SIZE)));
        }
        return hashes;
    }

    private long hashKGram(String gram) {
        long hash = 0;
        for (char c : gram.toCharArray()) {
            hash = hash * 31 + c;
        }
        return hash;
    }

    private List<Long> winnow(String text) {
        List<Long> hashes = kGrams(text);
        if (hashes.size() < WINDOW_SIZE) return hashes;

        List<Long> fingerprints = new ArrayList<>();
        for (int i = 0; i <= hashes.size() - WINDOW_SIZE; i++) {
            long min = Long.MAX_VALUE;
            for (int j = i; j < i + WINDOW_SIZE; j++) {
                min = Math.min(min, hashes.get(j));
            }
            if (fingerprints.isEmpty() || !fingerprints.get(fingerprints.size() - 1).equals(min)) {
                fingerprints.add(min);
            }
        }
        return fingerprints;
    }

    private double jaccardSimilarity(List<Long> a, List<Long> b) {
        if (a.isEmpty() && b.isEmpty()) return 0;
        Set<Long> setA = new HashSet<>(a);
        Set<Long> setB = new HashSet<>(b);
        Set<Long> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<Long> union = new HashSet<>(setA);
        union.addAll(setB);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private List<Map<String, Object>> findMatchingSections(String src1, String src2, String norm1, String norm2) {
        List<Map<String, Object>> sections = new ArrayList<>();
        String[] lines1 = src1.split("\n");
        String[] lines2 = src2.split("\n");

        for (int i = 0; i < lines1.length && sections.size() < 10; i++) {
            String trimmed1 = lines1[i].trim();
            if (trimmed1.length() < 15) continue;
            for (int j = 0; j < lines2.length; j++) {
                if (trimmed1.equals(lines2[j].trim())) {
                    Map<String, Object> match = new HashMap<>();
                    match.put("sourceLine", i + 1);
                    match.put("matchedLine", j + 1);
                    match.put("content", trimmed1.substring(0, Math.min(100, trimmed1.length())));
                    sections.add(match);
                    break;
                }
            }
        }
        return sections;
    }
}
