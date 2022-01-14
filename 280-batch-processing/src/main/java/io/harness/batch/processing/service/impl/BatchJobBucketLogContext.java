package io.harness.batch.processing.service.impl;

import io.harness.logging.AutoLogContext;

public class BatchJobBucketLogContext extends AutoLogContext {
  public static final String ID = "batchJobBucket";

  public BatchJobBucketLogContext(String batchJobBucket, AutoLogContext.OverrideBehavior behavior) {
    super(ID, batchJobBucket, behavior);
  }
}
