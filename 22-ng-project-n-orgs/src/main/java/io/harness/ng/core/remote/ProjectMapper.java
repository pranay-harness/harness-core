package io.harness.ng.core.remote;

import static io.harness.NGConstants.HARNESS_BLUE;

import static java.util.Collections.emptyList;

import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.entities.Project;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProjectMapper {
  public static Project toProject(ProjectDTO createProjectDTO) {
    return Project.builder()
        .accountIdentifier(createProjectDTO.getAccountIdentifier())
        .orgIdentifier(createProjectDTO.getOrgIdentifier())
        .identifier(createProjectDTO.getIdentifier())
        .name(createProjectDTO.getName())
        .description(Optional.ofNullable(createProjectDTO.getDescription()).orElse(""))
        .owners(Optional.ofNullable(createProjectDTO.getOwners()).orElse(emptyList()))
        .color(Optional.ofNullable(createProjectDTO.getColor()).orElse(HARNESS_BLUE))
        .tags(Optional.ofNullable(createProjectDTO.getTags()).orElse(emptyList()))
        .modules(Optional.ofNullable(createProjectDTO.getModules()).orElse(emptyList()))
        .build();
  }

  public static ProjectDTO writeDTO(Project project) {
    return ProjectDTO.builder()
        .accountIdentifier(project.getAccountIdentifier())
        .orgIdentifier(project.getOrgIdentifier())
        .identifier(project.getIdentifier())
        .name(project.getName())
        .description(project.getDescription())
        .owners(project.getOwners())
        .color(project.getColor())
        .tags(project.getTags())
        .modules(project.getModules())
        .lastModifiedAt(project.getLastModifiedAt())
        .build();
  }

  @SneakyThrows
  public static Project applyUpdateToProject(Project project, ProjectDTO updateProjectDTO) {
    String jsonString = new ObjectMapper().writer().writeValueAsString(updateProjectDTO);
    return new ObjectMapper().readerForUpdating(project).readValue(jsonString);
  }
}
