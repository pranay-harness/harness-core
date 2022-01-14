package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;

@OwnedBy(HarnessTeam.PIPELINE)
public class AsyncWaitEngineImpl implements AsyncWaitEngine {
  private final WaitNotifyEngine waitNotifyEngine;
  private final String publisherName;

  public AsyncWaitEngineImpl(WaitNotifyEngine waitNotifyEngine, String publisherName) {
    this.waitNotifyEngine = waitNotifyEngine;
    this.publisherName = publisherName;
  }

  @Override
  public void waitForAllOn(NotifyCallback notifyCallback, ProgressCallback progressCallback, String... correlationIds) {
    waitNotifyEngine.waitForAllOn(publisherName, notifyCallback, progressCallback, correlationIds);
  }
}
