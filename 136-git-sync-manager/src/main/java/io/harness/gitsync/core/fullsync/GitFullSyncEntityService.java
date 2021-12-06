package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;

import java.util.List;

@OwnedBy(DX)
public interface GitFullSyncEntityService {
  GitFullSyncEntityInfo save(GitFullSyncEntityInfo gitFullSyncEntityInfo);

  void markQueuedOrFailed(String uuid, String accountId, long currentRetryCount, long maxRetryCount, String errorMsg);

  void markSuccessful(String uuid, String accountId);

  List<GitFullSyncEntityInfo> list(String accountIdentifier, String fullSyncJobId);
}
