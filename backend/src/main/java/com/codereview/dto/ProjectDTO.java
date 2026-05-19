package com.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

public class ProjectDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Project name is required")
        @Size(max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        @Size(max = 50)
        private String language;

        private Boolean isPublic = false;

        @Size(max = 500)
        private String tags;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRequest {
        @Size(max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        @Size(max = 50)
        private String language;

        private Boolean isPublic;

        @Size(max = 500)
        private String tags;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private String language;
        private Boolean isPublic;
        private String tags;
        private String ownerUsername;
        private Long ownerId;
        private Long submissionCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
