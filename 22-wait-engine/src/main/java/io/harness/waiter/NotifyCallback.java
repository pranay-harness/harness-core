package io.harness.waiter;

import io.harness.delegate.beans.DelegateResponseData;

import java.util.Map;

/**
 * Function to call when all correlationIds are completed for a wait instance.
 */
public interface NotifyCallback {
  void notify(Map<String, DelegateResponseData> response);
  void notifyError(Map<String, DelegateResponseData> response);
}
