package io.harness.connector.mappers.jira;

import io.harness.connector.entities.embedded.jira.JiraConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.ng.core.NGAccess;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class JiraDTOToEntity implements ConnectorDTOToEntityMapper<JiraConnectorDTO, JiraConnector> {
  private SecretRefService secretRefService;

  @Override
  public JiraConnector toConnectorEntity(JiraConnectorDTO configDTO, NGAccess ngAccess) {
    return JiraConnector.builder()
        .jiraUrl(configDTO.getJiraUrl())
        .username(configDTO.getUsername())
        .passwordRef(secretRefService.validateAndGetSecretConfigString(configDTO.getPasswordRef(), ngAccess))
        .build();
  }
}
