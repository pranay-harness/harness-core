package io.harness.connector.mappers.gitconnectormapper;

import com.google.inject.Singleton;

import io.harness.connector.apis.dto.gitconnector.GitConfigSummaryDTO;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.mappers.ConnectorConfigSummaryDTOMapper;

@Singleton
public class GitConfigSummaryMapper implements ConnectorConfigSummaryDTOMapper<GitConfig> {
  public GitConfigSummaryDTO toConnectorConfigSummaryDTO(GitConfig connector) {
    return GitConfigSummaryDTO.builder().url(connector.getUrl()).build();
  }
}
