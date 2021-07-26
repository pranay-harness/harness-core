package io.harness.connector.mappers.jira;

import io.harness.connector.entities.embedded.jira.JiraConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
public class JiraEntityToDTO implements ConnectorEntityToDTOMapper<JiraConnectorDTO, JiraConnector> {
  @Override
  public JiraConnectorDTO createConnectorDTO(JiraConnector jiraConnector) {
    return JiraConnectorDTO.builder()
        .jiraUrl(jiraConnector.getJiraUrl())
        .username(jiraConnector.getUsername())
        .usernameRef(SecretRefHelper.createSecretRef(jiraConnector.getUsernameRef()))
        .passwordRef(SecretRefHelper.createSecretRef(jiraConnector.getPasswordRef()))
        .build();
  }
}
