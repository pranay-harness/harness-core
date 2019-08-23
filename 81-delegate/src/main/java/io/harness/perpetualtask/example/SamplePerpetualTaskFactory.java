package io.harness.perpetualtask.example;

import io.harness.perpetualtask.PerpetualTask;
import io.harness.perpetualtask.PerpetualTaskFactory;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;

public class SamplePerpetualTaskFactory implements PerpetualTaskFactory {
  @Override
  public PerpetualTask newTask(PerpetualTaskId taskId, PerpetualTaskParams params) throws Exception {
    return new SamplePerpetualTask(taskId, params);
  }
}
