package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.dtos.GitSyncSettingsDTO;

import java.util.Optional;

@OwnedBy(DX)
public interface GitSyncSettingsService {
  Optional<GitSyncSettingsDTO> get(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  GitSyncSettingsDTO save(GitSyncSettingsDTO request);

  GitSyncSettingsDTO update(GitSyncSettingsDTO request);
}
