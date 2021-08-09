package io.harness.batch.processing.service.impl;

import io.harness.logging.AutoLogContext;

public class BatchJobRunningModeContext extends AutoLogContext {
  public static final String ID = "batchJobRunningMode";

  public BatchJobRunningModeContext(boolean batchJobRunningMode, OverrideBehavior behavior) {
    super(ID, String.valueOf(batchJobRunningMode), behavior);
  }
}
