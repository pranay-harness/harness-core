package io.harness.pms.events.base;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.queue.QueueController;

@OwnedBy(HarnessTeam.PIPELINE)
public class NoopQueueController implements QueueController {
  @Override
  public boolean isPrimary() {
    return true;
  }

  @Override
  public boolean isNotPrimary() {
    return false;
  }
}
