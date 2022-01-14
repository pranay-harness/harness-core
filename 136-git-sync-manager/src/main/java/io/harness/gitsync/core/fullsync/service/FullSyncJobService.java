package io.harness.gitsync.core.fullsync.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob;

import java.util.Optional;

@OwnedBy(DX)
public interface FullSyncJobService {
  GitFullSyncJob save(GitFullSyncJob gitFullSyncJob);

  void markFullSyncJobAsFailed(String accountIdentifier, String uuid, GitFullSyncJob.SyncStatus status);

  void markFullSyncJobAsSuccess(String accountIdentifier, String uuid);

  Optional<GitFullSyncJob> getRunningJobs(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
