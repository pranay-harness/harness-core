/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.events.base;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.queue.QueueController;

@OwnedBy(HarnessTeam.PIPELINE)
public class NoopQueueController implements QueueController {
  @Override
  public boolean isPrimary() {
    return true;
  }

  @Override
  public boolean isNotPrimary() {
    return false;
  }
}
