/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import io.harness.tasks.ProgressData;

/**
 * Function to call when all correlationIds are completed for a wait instance.
 */
public interface ProgressCallback {
  void notify(String correlationId, ProgressData progressData);
}
