/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import io.harness.tasks.ResponseData;

import java.util.Map;

public interface PushThroughNotifyCallback extends NotifyCallback {
  void push(Map<String, ResponseData> response);
}
