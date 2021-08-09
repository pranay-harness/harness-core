
package io.harness.pms.sdk.core.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public interface OrchestrationEventHandler {
  void handleEvent(OrchestrationEvent event);
}
