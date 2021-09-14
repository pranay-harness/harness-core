/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service.impl;

import io.harness.logging.AutoLogContext;

public class BatchJobTypeLogContext extends AutoLogContext {
  public static final String ID = "batchJobType";

  public BatchJobTypeLogContext(String batchJobType, OverrideBehavior behavior) {
    super(ID, batchJobType, behavior);
  }
}
