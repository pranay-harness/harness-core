package io.harness.pms.sdk.core.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
// TODO (prashant): This is deprecated all of these needs to be moved to Observers
@Deprecated
public interface SyncOrchestrationEventHandler extends OrchestrationEventHandler {}
