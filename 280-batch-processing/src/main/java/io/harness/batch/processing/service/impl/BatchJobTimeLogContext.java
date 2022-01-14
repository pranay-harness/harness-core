package io.harness.batch.processing.service.impl;

import io.harness.logging.AutoLogContext;

public class BatchJobTimeLogContext extends AutoLogContext {
  public static final String ID = "batchJobTime";

  public BatchJobTimeLogContext(String batchJobTime, OverrideBehavior behavior) {
    super(ID, batchJobTime, behavior);
  }
}
