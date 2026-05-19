package com.codereview.repository;

import com.codereview.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Page<Project> findByOwnerId(Long ownerId, Pageable pageable);
    Page<Project> findByOwnerIdAndNameContainingIgnoreCase(Long ownerId, String name, Pageable pageable);
    Page<Project> findByIsPublicTrue(Pageable pageable);
    long countByOwnerId(Long ownerId);

    @Query("SELECT p FROM Project p WHERE p.owner.id = :ownerId OR p.isPublic = true")
    Page<Project> findAccessibleProjects(Long ownerId, Pageable pageable);
}
