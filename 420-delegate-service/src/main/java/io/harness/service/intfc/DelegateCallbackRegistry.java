/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.intfc;

import io.harness.callback.DelegateCallback;

public interface DelegateCallbackRegistry {
  String ensureCallback(DelegateCallback delegateCallback);
  DelegateCallbackService obtainDelegateCallbackService(String driverId);

  DelegateTaskResultsProvider obtainDelegateTaskResultsProvider(String driverId);

  DelegateTaskResultsProvider buildDelegateTaskResultsProvider(String driverId);
}
