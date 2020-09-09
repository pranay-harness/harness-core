package io.harness.service.intfc;

import io.harness.callback.DelegateCallback;

public interface DelegateCallbackRegistry {
  String ensureCallback(DelegateCallback delegateCallback);
  DelegateCallbackService obtainDelegateCallbackService(String driverId);

  DelegateTaskResultsProvider obtainDelegateTaskResultsProvider(String driverId);

  DelegateTaskResultsProvider buildDelegateTaskResultsProvider(String driverId);
}
