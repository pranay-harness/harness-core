package io.harness.pms.sdk.core;

import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.ProgressCallback;

public class TestAsyncWaitEngineImpl implements AsyncWaitEngine {
  @Override
  public void waitForAllOn(NotifyCallback notifyCallback, ProgressCallback progressCallback, String... correlationIds) {
    // Do nothing
  }
}
