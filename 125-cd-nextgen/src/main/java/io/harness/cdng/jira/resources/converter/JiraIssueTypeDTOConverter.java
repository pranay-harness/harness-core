package io.harness.cdng.jira.resources.converter;

import io.harness.cdng.jira.resources.response.dto.JiraIssueTypeDTO;
import io.harness.jira.JiraIssueType;

import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JiraIssueTypeDTOConverter {
  public Function<JiraIssueType, JiraIssueTypeDTO> toJiraIssueTypeDTO = jiraIssueType
      -> JiraIssueTypeDTO.builder()
             .id(jiraIssueType.getId())
             .name(jiraIssueType.getName())
             .description(jiraIssueType.getDescription())
             .isSubTask(jiraIssueType.isSubTask())
             .jiraStatusList(jiraIssueType.getJiraStatusList()
                                 .stream()
                                 .map(JiraStatusDTOConverter.toJiraStatusDTO)
                                 .collect(Collectors.toList()))
             .build();
}
