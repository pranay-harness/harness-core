package io.harness.connector.mappers.jira;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.jira.JiraConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class JiraEntityToDTO implements ConnectorEntityToDTOMapper<JiraConnectorDTO, JiraConnector> {
  @Override
  public JiraConnectorDTO createConnectorDTO(JiraConnector jiraConnector) {
    return JiraConnectorDTO.builder()
        .jiraUrl(jiraConnector.getJiraUrl())
        .username(jiraConnector.getUsername())
        .passwordRef(SecretRefHelper.createSecretRef(jiraConnector.getPasswordRef()))
        .build();
  }
}
