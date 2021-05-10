package io.harness.waiter;

import io.harness.tasks.ResponseData;

import java.util.Map;

public interface PushThroughNotifyCallback extends NotifyCallback {
  void push(Map<String, ResponseData> response);
}