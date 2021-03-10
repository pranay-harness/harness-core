package io.harness.ng.core.remote;

import static io.harness.NGConstants.HARNESS_BLUE;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;

import static java.util.Collections.emptyList;

import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.ng.core.entities.Project;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProjectMapper {
  public static Project toProject(ProjectDTO createProjectDTO) {
    return Project.builder()
        .orgIdentifier(createProjectDTO.getOrgIdentifier())
        .identifier(createProjectDTO.getIdentifier())
        .name(createProjectDTO.getName())
        .description(Optional.ofNullable(createProjectDTO.getDescription()).orElse(""))
        .color(Optional.ofNullable(createProjectDTO.getColor()).orElse(HARNESS_BLUE))
        .tags(convertToList(createProjectDTO.getTags()))
        .version(createProjectDTO.getVersion())
        .modules(Optional.ofNullable(createProjectDTO.getModules()).orElse(emptyList()))
        .build();
  }

  public static ProjectDTO writeDTO(Project project) {
    return ProjectDTO.builder()
        .orgIdentifier(project.getOrgIdentifier())
        .identifier(project.getIdentifier())
        .name(project.getName())
        .description(project.getDescription())
        .color(project.getColor())
        .tags(convertToMap(project.getTags()))
        .modules(project.getModules())
        .build();
  }

  public static ProjectResponse toResponseWrapper(Project project) {
    return ProjectResponse.builder()
        .createdAt(project.getCreatedAt())
        .lastModifiedAt(project.getLastModifiedAt())
        .project(writeDTO(project))
        .build();
  }

  @SneakyThrows
  public static Project applyUpdateToProject(Project project, ProjectDTO updateProjectDTO) {
    String jsonString = new ObjectMapper().writer().writeValueAsString(updateProjectDTO);
    return new ObjectMapper().readerForUpdating(project).readValue(jsonString);
  }
}
