package io.harness.perpetualtask.example;

import com.google.inject.Singleton;

import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Singleton
@Slf4j
public class SamplePerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Override
  public boolean startTask(PerpetualTaskId taskId, PerpetualTaskParams params, Instant heartbeatTime) throws Exception {
    SamplePerpetualTaskParams sampleParams = params.getCustomizedParams().unpack(SamplePerpetualTaskParams.class);
    logger.info("Hello there !! {} ", sampleParams.getCountry());
    return true;
  }

  @Override
  public boolean stopTask(PerpetualTaskId taskId, PerpetualTaskParams params) {
    return true;
  }
}
