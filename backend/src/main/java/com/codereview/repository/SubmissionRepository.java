package com.codereview.repository;

import com.codereview.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Page<Submission> findByProjectId(Long projectId, Pageable pageable);
    Page<Submission> findByUserId(Long userId, Pageable pageable);
    List<Submission> findByProjectIdAndLanguage(Long projectId, String language);
    long countByUserId(Long userId);
    long countByProjectId(Long projectId);
    long countByStatus(Submission.SubmissionStatus status);

    @Query("SELECT s FROM Submission s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<Submission> findRecentByUserId(Long userId, Pageable pageable);

    @Query("SELECT s.language, COUNT(s) FROM Submission s WHERE s.user.id = :userId GROUP BY s.language")
    List<Object[]> countByLanguageForUser(Long userId);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.createdAt >= :since")
    long countSince(LocalDateTime since);

    @Query("SELECT s FROM Submission s WHERE s.language = :language AND s.id != :excludeId AND s.project.id = :projectId")
    List<Submission> findSimilarSubmissions(Long projectId, String language, Long excludeId);
}
