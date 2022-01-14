package io.harness.waiter;

import io.harness.tasks.ResponseData;

import java.util.Map;

/**
 * Function to call when all correlationIds are completed for a wait instance.
 */
public interface OldNotifyCallback extends NotifyCallback {
  void notify(Map<String, ResponseData> response);
  void notifyError(Map<String, ResponseData> response);
}
