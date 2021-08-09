package io.harness.waiter;

import io.harness.tasks.ResponseData;

import java.util.Map;

public interface NotifyCallback {
  default void notifyTimeout(Map<String, ResponseData> responseMap) {}
}
