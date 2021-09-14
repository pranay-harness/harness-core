/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import io.harness.tasks.ResponseData;

import java.util.Map;
import java.util.function.Supplier;

public interface NotifyCallbackWithErrorHandling extends NotifyCallback {
  void notify(Map<String, Supplier<ResponseData> > response);
}
