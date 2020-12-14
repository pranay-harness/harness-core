package io.harness.waiter;

import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;

public class AsyncWaitEngineImpl implements AsyncWaitEngine {
  private final WaitNotifyEngine waitNotifyEngine;
  private final String publisherName;

  public AsyncWaitEngineImpl(WaitNotifyEngine waitNotifyEngine, String publisherName) {
    this.waitNotifyEngine = waitNotifyEngine;
    this.publisherName = publisherName;
  }

  @Override
  public void waitForAllOn(NotifyCallback notifyCallback, String... correlationIds) {
    waitNotifyEngine.waitForAllOn(publisherName, notifyCallback, correlationIds);
  }
}
