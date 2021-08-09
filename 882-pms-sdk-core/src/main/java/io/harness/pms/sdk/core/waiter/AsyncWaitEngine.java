package io.harness.pms.sdk.core.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.ProgressCallback;

@OwnedBy(HarnessTeam.PIPELINE)
public interface AsyncWaitEngine {
  void waitForAllOn(NotifyCallback notifyCallback, ProgressCallback progressCallback, String... correlationIds);
}
