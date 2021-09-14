/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sdk.core;

import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.ProgressCallback;

public class TestAsyncWaitEngineImpl implements AsyncWaitEngine {
  @Override
  public void waitForAllOn(NotifyCallback notifyCallback, ProgressCallback progressCallback, String... correlationIds) {
    // Do nothing
  }
}
