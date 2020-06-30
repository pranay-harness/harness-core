package io.harness.ng.core.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.ng.core.dto.CreateProjectDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.UpdateProjectDTO;
import io.harness.ng.core.entities.Project;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProjectMapper {
  static Project toProject(CreateProjectDTO createProjectDTO) {
    return Project.builder()
        .identifier(createProjectDTO.getIdentifier())
        .accountIdentifier(createProjectDTO.getAccountIdentifier())
        .name(createProjectDTO.getName())
        .description(createProjectDTO.getDescription())
        .owners(createProjectDTO.getOwners())
        .color(createProjectDTO.getColor())
        .tags(createProjectDTO.getTags())
        .purposeList(createProjectDTO.getPurposeList())
        .build();
  }

  static ProjectDTO writeDTO(Project project) {
    return ProjectDTO.builder()
        .id(project.getId())
        .accountIdentifier(project.getAccountIdentifier())
        .orgIdentifier(project.getOrgIdentifier())
        .identifier(project.getIdentifier())
        .name(project.getName())
        .description(project.getDescription())
        .owners(project.getOwners())
        .color(project.getColor())
        .tags(project.getTags())
        .purposeList(project.getPurposeList())
        .build();
  }

  @SneakyThrows
  static Project applyUpdateToProject(Project project, UpdateProjectDTO updateProjectDTO) {
    String jsonString = new ObjectMapper().writer().writeValueAsString(updateProjectDTO);
    return new ObjectMapper().readerForUpdating(project).readValue(jsonString);
  }
}
