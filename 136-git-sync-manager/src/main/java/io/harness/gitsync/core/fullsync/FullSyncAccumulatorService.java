package io.harness.gitsync.core.fullsync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;

@OwnedBy(HarnessTeam.DX)
public interface FullSyncAccumulatorService {
  void triggerFullSync(EntityScopeInfo entityScopeInfo, String messageId);
}
