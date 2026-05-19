package com.codereview.repository;

import com.codereview.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FindingRepository extends JpaRepository<Finding, Long> {
    List<Finding> findByReviewId(Long reviewId);

    @Query("SELECT f.severity, COUNT(f) FROM Finding f WHERE f.review.submission.user.id = :userId GROUP BY f.severity")
    List<Object[]> countBySeverityForUser(Long userId);
}
