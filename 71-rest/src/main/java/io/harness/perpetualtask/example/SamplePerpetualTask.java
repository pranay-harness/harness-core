package io.harness.perpetualtask.example;

import io.harness.perpetualtask.AbstractPerpetualTask;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.example.SampleTask.SamplePerpetualTaskParams;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SamplePerpetualTask extends AbstractPerpetualTask {
  private String country;

  public SamplePerpetualTask(PerpetualTaskId taskId, SamplePerpetualTaskParams params) {
    super(taskId);
    this.country = params.getCountry();
  }

  @Override
  public void run() {
    logger.info("Hello " + country);
  }
}
