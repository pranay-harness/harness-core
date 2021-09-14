/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask.example;

import static java.lang.Thread.sleep;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.grpc.utils.AnyUtils;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;

import com.google.inject.Singleton;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
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
