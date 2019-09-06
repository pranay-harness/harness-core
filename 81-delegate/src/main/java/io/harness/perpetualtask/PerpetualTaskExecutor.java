package io.harness.perpetualtask;

import java.time.Instant;

public interface PerpetualTaskExecutor {
  boolean startTask(PerpetualTaskId taskId, PerpetualTaskParams params, Instant heartbeatTime) throws Exception;
  boolean stopTask(PerpetualTaskId taskId, PerpetualTaskParams params) throws Exception;
}
