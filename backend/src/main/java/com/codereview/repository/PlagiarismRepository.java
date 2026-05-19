package com.codereview.repository;

import com.codereview.entity.PlagiarismResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PlagiarismRepository extends JpaRepository<PlagiarismResult, Long> {
    Optional<PlagiarismResult> findBySubmissionId(Long submissionId);
}
