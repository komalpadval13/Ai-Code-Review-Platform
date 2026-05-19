package com.codereview.repository;

import com.codereview.entity.CodeMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MetricsRepository extends JpaRepository<CodeMetrics, Long> {
    Optional<CodeMetrics> findBySubmissionId(Long submissionId);

    @Query("SELECT AVG(m.maintainabilityIndex) FROM CodeMetrics m WHERE m.submission.user.id = :userId")
    Double averageMaintainabilityByUserId(Long userId);
}
