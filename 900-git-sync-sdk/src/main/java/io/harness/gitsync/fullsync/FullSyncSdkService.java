package io.harness.gitsync.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.FileChanges;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.ScopeDetails;

@OwnedBy(DX)
public interface FullSyncSdkService {
  FileChanges getFileChanges(ScopeDetails scopeDetails);

  void doFullSyncForFile(FullSyncChangeSet fullSyncChangeSet);
}
