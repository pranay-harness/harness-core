package io.harness.delegate.task.jira.mappers;

import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.jira.JiraInternalConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
public class JiraRequestResponseMapper {
  public JiraInternalConfig toJiraInternalConfig(JiraTaskNGParameters parameters) {
    JiraConnectorDTO dto = parameters.getJiraConnectorDTO();
    return JiraInternalConfig.builder()
        .jiraUrl(dto.getJiraUrl())
        .username(dto.getUsername())
        .password(new String(dto.getPasswordRef().getDecryptedValue()))
        .build();
  }
}
