package io.harness.pms.sdk.core.waiter;

import io.harness.waiter.NotifyCallback;

public interface AsyncWaitEngine {
  void waitForAllOn(NotifyCallback notifyCallback, String... correlationIds);
}
