package io.harness.connector.mappers;

import com.google.inject.Singleton;

import io.harness.connector.apis.dto.gitconnector.GitConfigSummaryDTO;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;

@Singleton
public class GitConfigSummaryMapper {
  public GitConfigSummaryDTO createGitConfigSummaryDTO(GitConfig connector) {
    return GitConfigSummaryDTO.builder().url(connector.getUrl()).build();
  }
}
