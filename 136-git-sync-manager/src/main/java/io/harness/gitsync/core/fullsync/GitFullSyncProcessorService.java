package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.core.fullsync.beans.GitFullSyncEntityProcessingResponse;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob;

@OwnedBy(DX)
public interface GitFullSyncProcessorService {
  GitFullSyncEntityProcessingResponse processFile(GitFullSyncEntityInfo entityInfo);

  void performFullSync(GitFullSyncJob fullSyncJob);
}
