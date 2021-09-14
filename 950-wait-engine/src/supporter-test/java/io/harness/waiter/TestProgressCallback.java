/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.ProgressData;

@OwnedBy(HarnessTeam.PIPELINE)
public class TestProgressCallback implements ProgressCallback {
  @Override
  public void notify(String correlationId, ProgressData progressData) {
    // NOOP
  }
}
