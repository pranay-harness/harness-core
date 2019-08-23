package io.harness.perpetualtask;

public interface PerpetualTaskFactory {
  PerpetualTask newTask(PerpetualTaskId taskId, PerpetualTaskParams params) throws Exception;
}
