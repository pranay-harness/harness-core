package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DX)
public interface GitSyncSdkService {
  boolean isGitSyncEnabled(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  boolean isDefaultBranch(String accountId, String orgIdentifier, String projectIdentifier);
}
