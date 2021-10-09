package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;

@OwnedBy(DX)
public interface GitFullSyncProcessorService {
  void processFile(GitFullSyncEntityInfo entityInfo);
}
