package io.harness.ng.core.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.ng.core.dto.CreateProjectRequest;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.UpdateProjectRequest;
import io.harness.ng.core.entities.Project;
import lombok.SneakyThrows;

public class ProjectMapper {
  Project toProject(CreateProjectRequest request) {
    return Project.builder()
        .accountId(request.getAccountId())
        .orgId(request.getOrgId())
        .identifier(request.getIdentifier())
        .name(request.getName())
        .description(request.getDescription())
        .owners(request.getOwners())
        .tags(request.getTags())
        .build();
  }

  ProjectDTO writeDTO(Project project) {
    return ProjectDTO.builder()
        .uuid(project.getUuid())
        .accountId(project.getAccountId())
        .orgId(project.getOrgId())
        .identifier(project.getIdentifier())
        .name(project.getName())
        .description(project.getDescription())
        .owners(project.getOwners())
        .tags(project.getTags())
        .build();
  }

  @SneakyThrows
  Project applyUpdateToProject(Project project, UpdateProjectRequest request) {
    String jsonString = new ObjectMapper().writer().writeValueAsString(request);
    return new ObjectMapper().readerForUpdating(project).readValue(jsonString);
  }
}
