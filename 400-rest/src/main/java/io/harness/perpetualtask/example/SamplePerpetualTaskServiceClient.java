/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask.example;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SamplePerpetualTaskServiceClient implements PerpetualTaskServiceClient {
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private SamplePTaskService samplePTaskService;
  static final String COUNTRY_NAME = "countryName";

  @Override
  public SamplePerpetualTaskParams getTaskParams(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    final String countryName = clientParams.get(COUNTRY_NAME);
    int population = samplePTaskService.getPopulation(countryName);
    log.info("Country = [{}], population = [{}]", countryName, population);
    return SamplePerpetualTaskParams.newBuilder().setCountry(countryName).setPopulation(population).build();
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext context, String accountId) {
    return DelegateTask.builder()
        .accountId(accountId)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.HTTP.name())
                  .parameters(new Object[] {HttpTaskParameters.builder().url("https://www.google.com").build()})
                  .timeout(TimeUnit.MINUTES.toMillis(1))
                  .build())
        .build();
  }
}
