package io.harness.cdng.jira.resources.converter;

import io.harness.cdng.jira.resources.response.dto.JiraProjectDTO;
import io.harness.jira.JiraProjectData;
import lombok.experimental.UtilityClass;

import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class JiraProjectDTOConverter {
  public Function<JiraProjectData, JiraProjectDTO> toJiraProjectDTO = project
      -> JiraProjectDTO.builder()
             .id(project.getId())
             .key(project.getKey())
             .name(project.getName())
             .issueTypes(project.getIssueTypes()
                             .stream()
                             .map(JiraIssueTypeDTOConverter.toJiraIssueTypeDTO)
                             .collect(Collectors.toList()))
             .build();
}
