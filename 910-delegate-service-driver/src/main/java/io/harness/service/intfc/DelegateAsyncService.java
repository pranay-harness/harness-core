package io.harness.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateAsyncService extends Runnable {
  void setupTimeoutForTask(String taskId, long expiry, long holdUntil);
}
