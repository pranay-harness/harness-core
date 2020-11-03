package io.harness.cdng.jira.resources.converter;

import io.harness.cdng.jira.resources.response.JiraIssueDTO;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import lombok.experimental.UtilityClass;

import java.util.function.Function;

@UtilityClass
public class JiraIssueDTOConverter {
  public Function<JiraTaskNGResponse, JiraIssueDTO> toJiraIssueDTO() {
    return response
        -> JiraIssueDTO.builder()
               .issueKey(response.getIssueKey())
               .issueUrl(response.getIssueUrl())
               .executionStatus(response.getExecutionStatus().name())
               .currentStatus(response.getCurrentStatus())
               .description(response.getJiraIssueData().getDescription())
               .errorMessage(response.getErrorMessage())
               .build();
  }
}
