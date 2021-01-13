package io.harness.connector.mappers.jira;

import static io.harness.connector.ConnectorCategory.TICKETING;

import io.harness.connector.ConnectorCategory;
import io.harness.connector.entities.embedded.jira.JiraConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Singleton
public class JiraDTOToEntity extends ConnectorDTOToEntityMapper<JiraConnectorDTO, JiraConnector> {
  @Override
  public JiraConnector toConnectorEntity(JiraConnectorDTO configDTO) {
    return JiraConnector.builder()
        .jiraUrl(configDTO.getJiraUrl())
        .username(configDTO.getUsername())
        .passwordRef(SecretRefHelper.getSecretConfigString(configDTO.getPasswordRef()))
        .build();
  }
}
