package io.harness.perpetualtask.example;

import static java.lang.Thread.sleep;

import com.google.inject.Singleton;

import io.harness.grpc.utils.AnyUtils;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Singleton
@Slf4j
public class SamplePerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    SamplePerpetualTaskParams sampleParams =
        AnyUtils.unpack(params.getCustomizedParams(), SamplePerpetualTaskParams.class);
    try {
      sleep(10000);
    } catch (InterruptedException e) {
      log.error("The Sample PTask is interrupted", e);
      Thread.currentThread().interrupt();
    }
    log.info("The country {} has a population of {}", sampleParams.getCountry(), sampleParams.getPopulation());
    return PerpetualTaskResponse.builder()
        .responseCode(ThreadLocalRandom.current().nextInt(1, 200))
        .responseMessage("success")
        .build();
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return true;
  }
}
