package io.harness.perpetualtask.example;

import io.harness.perpetualtask.AbstractPerpetualTask;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SamplePerpetualTask extends AbstractPerpetualTask {
  private String country;

  public SamplePerpetualTask(PerpetualTaskId taskId, PerpetualTaskParams params) throws Exception {
    super(taskId);
    SamplePerpetualTaskParams sampleParams = params.getCustomizedParams().unpack(SamplePerpetualTaskParams.class);
    this.country = sampleParams.getCountry();
  }

  @Override
  public Void call() throws Exception {
    logger.info("Hello " + country);
    return null;
  }

  @Override
  public void stop() {}
}
