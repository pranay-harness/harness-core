package io.harness.batch.processing.service.impl;

import io.harness.logging.AutoLogContext;

public class BatchJobTypeLogContext extends AutoLogContext {
  public static final String ID = "batchJobType";

  public BatchJobTypeLogContext(String batchJobType, OverrideBehavior behavior) {
    super(ID, batchJobType, behavior);
  }
}
