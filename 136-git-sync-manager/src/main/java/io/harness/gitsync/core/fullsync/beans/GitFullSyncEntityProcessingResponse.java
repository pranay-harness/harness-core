package io.harness.gitsync.core.fullsync.beans;

import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitFullSyncEntityProcessingResponse {
  GitFullSyncEntityInfo.SyncStatus syncStatus;
}
