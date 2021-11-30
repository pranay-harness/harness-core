package io.harness.gitsync.core.fullsync;

import io.harness.gitsync.fullsync.dtos.GitFullSyncConfigDTO;
import io.harness.gitsync.fullsync.dtos.GitFullSyncConfigRequestDTO;

import java.util.Optional;

public interface GitFullSyncConfigService {
  GitFullSyncConfigDTO createConfig(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, GitFullSyncConfigRequestDTO dto);

  Optional<GitFullSyncConfigDTO> get(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  GitFullSyncConfigDTO updateConfig(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, GitFullSyncConfigRequestDTO dto);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
