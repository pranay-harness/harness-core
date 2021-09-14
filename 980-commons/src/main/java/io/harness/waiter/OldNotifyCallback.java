/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
