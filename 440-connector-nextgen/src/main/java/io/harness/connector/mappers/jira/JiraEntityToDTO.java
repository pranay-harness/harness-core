package io.harness.connector.mappers.jira;

import io.harness.connector.entities.embedded.jira.JiraConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class JiraEntityToDTO implements ConnectorEntityToDTOMapper<JiraConnectorDTO, JiraConnector> {
  private SecretRefService secretRefService;

  @Override
  public JiraConnectorDTO createConnectorDTO(JiraConnector jiraConnector) {
    return JiraConnectorDTO.builder()
        .jiraUrl(jiraConnector.getJiraUrl())
        .username(jiraConnector.getUsername())
        .passwordRef(secretRefService.createSecretRef(jiraConnector.getPasswordRef()))
        .build();
  }
}
