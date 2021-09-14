/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.ResponseData;

import java.time.Duration;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateSyncService extends Runnable {
  <T extends ResponseData> T waitForTask(String taskId, String description, Duration timeout);
}
