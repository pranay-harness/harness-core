/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.intfc;

import io.harness.beans.DelegateTask;
import io.harness.service.dto.RetryDelegate;

public interface DelegateTaskRetryObserver {
  RetryDelegate onPossibleRetry(RetryDelegate retryDelegate);
  void onTaskResponseProcessed(DelegateTask delegateTask, String delegateId);
}
