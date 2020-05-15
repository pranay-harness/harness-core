package io.harness.perpetualtask;

import java.time.Instant;

public interface PerpetualTaskExecutor {
  // Specify what should be done in a single iteration of the task.
  PerpetualTaskResponse runOnce(PerpetualTaskId taskId, PerpetualTaskParams params, Instant heartbeatTime);

  // Cleanup any state that's maintained for a  task.
  boolean cleanup(PerpetualTaskId taskId, PerpetualTaskParams params);
}
