package io.harness.batch.processing.service.impl;

import io.harness.logging.AutoLogContext;

public class AnomalyDetectionLogContext extends AutoLogContext {
  public static final String ID = "AnomalyId";

  public AnomalyDetectionLogContext(String id, AutoLogContext.OverrideBehavior behavior) {
    super(ID, id, behavior);
  }
}
