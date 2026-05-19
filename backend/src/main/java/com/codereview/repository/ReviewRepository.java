package com.codereview.repository;

import com.codereview.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findBySubmissionId(Long submissionId);

    @Query("SELECT AVG(r.overallScore) FROM Review r WHERE r.submission.user.id = :userId")
    Double averageScoreByUserId(Long userId);

    @Query("SELECT SUM(r.totalIssues) FROM Review r WHERE r.submission.user.id = :userId")
    Long totalIssuesByUserId(Long userId);
}
