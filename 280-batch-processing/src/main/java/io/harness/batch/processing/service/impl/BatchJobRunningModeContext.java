/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service.impl;

import io.harness.logging.AutoLogContext;

public class BatchJobRunningModeContext extends AutoLogContext {
  public static final String ID = "batchJobRunningMode";

  public BatchJobRunningModeContext(boolean batchJobRunningMode, OverrideBehavior behavior) {
    super(ID, String.valueOf(batchJobRunningMode), behavior);
  }
}
