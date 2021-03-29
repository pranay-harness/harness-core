package io.harness.pms.sdk.core.waiter;

import io.harness.waiter.OldNotifyCallback;

public interface AsyncWaitEngine {
  void waitForAllOn(OldNotifyCallback notifyCallback, String... correlationIds);
}
