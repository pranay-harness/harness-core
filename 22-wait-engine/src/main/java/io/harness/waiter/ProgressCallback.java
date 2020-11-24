package io.harness.waiter;

import io.harness.tasks.ProgressData;

/**
 * Function to call when all correlationIds are completed for a wait instance.
 */
public interface ProgressCallback {
  void notify(String correlationId, ProgressData progressData);
}