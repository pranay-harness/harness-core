/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service.impl;

import io.harness.logging.AutoLogContext;

public class AnomalyDetectionLogContext extends AutoLogContext {
  public static final String ID = "AnomalyId";

  public AnomalyDetectionLogContext(String id, AutoLogContext.OverrideBehavior behavior) {
    super(ID, id, behavior);
  }
}
