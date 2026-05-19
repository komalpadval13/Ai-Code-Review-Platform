package com.codereview.service;

import com.codereview.dto.ProjectDTO;
import com.codereview.entity.Project;
import com.codereview.entity.User;
import com.codereview.exception.BadRequestException;
import com.codereview.exception.ResourceNotFoundException;
import com.codereview.repository.ProjectRepository;
import com.codereview.repository.SubmissionRepository;
import com.codereview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final AuditService auditService;

    @Transactional
    public ProjectDTO.Response createProject(Long userId, ProjectDTO.CreateRequest request) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Project project = Project.builder()
                .name(request.getName()).description(request.getDescription())
                .language(request.getLanguage())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .tags(request.getTags()).owner(user).build();
        project = projectRepository.save(Objects.requireNonNull(project));
        auditService.log(userId, user.getUsername(), "CREATE_PROJECT",
                "Created project: " + project.getName(), "Project", project.getId());
        return mapToResponse(project);
    }

    @Transactional
    public ProjectDTO.Response updateProject(Long userId, Long projectId, ProjectDTO.UpdateRequest request) {
        Project project = projectRepository.findById(Objects.requireNonNull(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        if (!project.getOwner().getId().equals(userId))
            throw new BadRequestException("No permission to update this project");
        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getLanguage() != null) project.setLanguage(request.getLanguage());
        if (request.getIsPublic() != null) project.setIsPublic(request.getIsPublic());
        if (request.getTags() != null) project.setTags(request.getTags());
        project = projectRepository.save(project);
        return mapToResponse(project);
    }

    @Transactional
    public void deleteProject(Long userId, Long projectId) {
        Project project = projectRepository.findById(Objects.requireNonNull(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        if (!project.getOwner().getId().equals(userId))
            throw new BadRequestException("No permission to delete this project");
        projectRepository.delete(project);
    }

    public ProjectDTO.Response getProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(Objects.requireNonNull(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        if (!project.getIsPublic() && !project.getOwner().getId().equals(userId))
            throw new BadRequestException("No permission to view this project");
        return mapToResponse(project);
    }

    public Page<ProjectDTO.Response> getUserProjects(Long userId, String search, Pageable pageable) {
        Page<Project> projects = (search != null && !search.isBlank())
                ? projectRepository.findByOwnerIdAndNameContainingIgnoreCase(userId, search, pageable)
                : projectRepository.findByOwnerId(userId, pageable);
        return projects.map(this::mapToResponse);
    }

    public Page<ProjectDTO.Response> getPublicProjects(Pageable pageable) {
        return projectRepository.findByIsPublicTrue(pageable).map(this::mapToResponse);
    }

    private ProjectDTO.Response mapToResponse(Project p) {
        long count = submissionRepository.countByProjectId(p.getId());
        return ProjectDTO.Response.builder().id(p.getId()).name(p.getName())
                .description(p.getDescription()).language(p.getLanguage())
                .isPublic(p.getIsPublic()).tags(p.getTags())
                .ownerUsername(p.getOwner().getUsername()).ownerId(p.getOwner().getId())
                .submissionCount(count).createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
}
