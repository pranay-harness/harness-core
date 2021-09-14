/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service.impl;

import io.harness.logging.AutoLogContext;

public class BatchJobBucketLogContext extends AutoLogContext {
  public static final String ID = "batchJobBucket";

  public BatchJobBucketLogContext(String batchJobBucket, AutoLogContext.OverrideBehavior behavior) {
    super(ID, batchJobBucket, behavior);
  }
}
