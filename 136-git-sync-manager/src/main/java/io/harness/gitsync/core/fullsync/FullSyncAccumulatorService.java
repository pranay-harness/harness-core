package io.harness.gitsync.core.fullsync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.FullSyncEventRequest;

@OwnedBy(HarnessTeam.DX)
public interface FullSyncAccumulatorService {
  void triggerFullSync(FullSyncEventRequest fullSyncEventRequest, String messageId);
}
