/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.tasklet.DataCheckBigqueryAndTimescaleTasklet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OwnedBy(HarnessTeam.CE)
@Slf4j
@Configuration
public class DataCheckBigqueryAndTimescaleConfig {
  @Bean
  public Tasklet dataCheckBigqueryAndTimescaleTasklet() {
    return new DataCheckBigqueryAndTimescaleTasklet();
  }

  @Bean
  public Step dataCheckBigqueryAndTimescaleStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("dataCheckBigqueryAndTimescaleStep")
        .tasklet(dataCheckBigqueryAndTimescaleTasklet())
        .build();
  }

  @Bean
  @Qualifier(value = "dataCheckBigqueryAndTimescaleJob")
  public Job dataCheckBigqueryAndTimescaleJob(
      JobBuilderFactory jobBuilderFactory, Step dataCheckBigqueryAndTimescaleStep) {
    return jobBuilderFactory.get(BatchJobType.DATA_CHECK_BIGQUERY_TIMESCALE.name())
        .incrementer(new RunIdIncrementer())
        .start(dataCheckBigqueryAndTimescaleStep)
        .build();
  }
}
