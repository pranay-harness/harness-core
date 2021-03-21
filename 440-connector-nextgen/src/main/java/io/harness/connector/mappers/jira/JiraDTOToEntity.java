package io.harness.connector.mappers.jira;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.jira.JiraConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class JiraDTOToEntity implements ConnectorDTOToEntityMapper<JiraConnectorDTO, JiraConnector> {
  @Override
  public JiraConnector toConnectorEntity(JiraConnectorDTO configDTO) {
    return JiraConnector.builder()
        .jiraUrl(configDTO.getJiraUrl())
        .username(configDTO.getUsername())
        .passwordRef(SecretRefHelper.getSecretConfigString(configDTO.getPasswordRef()))
        .build();
  }
}
