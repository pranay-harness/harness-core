package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.ProgressData;

@OwnedBy(HarnessTeam.PIPELINE)
public class TestProgressCallback implements ProgressCallback {
  @Override
  public void notify(String correlationId, ProgressData progressData) {
    // NOOP
  }
}
