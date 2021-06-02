package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.SdkResponseEvent;

@OwnedBy(HarnessTeam.PIPELINE)
public interface SdkResponseEventHandler {
  void handleEvent(SdkResponseEvent event);
}
