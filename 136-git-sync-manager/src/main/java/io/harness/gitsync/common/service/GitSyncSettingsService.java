package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.dtos.GitSyncSettingsDTO;

@OwnedBy(DX)
public interface GitSyncSettingsService {
  GitSyncSettingsDTO get(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  GitSyncSettingsDTO save(GitSyncSettingsDTO request);
}
