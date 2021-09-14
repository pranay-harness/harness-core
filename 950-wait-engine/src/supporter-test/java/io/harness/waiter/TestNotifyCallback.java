/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.ResponseData;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class TestNotifyCallback implements OldNotifyCallback {
  @Override
  public void notify(Map<String, ResponseData> response) {
    // NOOP
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    // NOOP
  }
}
