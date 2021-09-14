/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.schedule;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.metrics.CeProductMetricsTasklet;
import io.harness.batch.processing.metrics.ProductMetricsService;

import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SegmentJobConfiguration {
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private ProductMetricsService productMetricsService;

  @Bean
  public Tasklet ceProductMetricsTasklet() {
    return new CeProductMetricsTasklet();
  }

  @Bean
  @Autowired
  @Qualifier(value = "ceProductMetricsJob")
  public Job ceProductMetricsJob(JobBuilderFactory jobBuilderFactory, Step ceProductMetricsStep) {
    return jobBuilderFactory.get(BatchJobType.CE_SEGMENT_CALL.name())
        .incrementer(new RunIdIncrementer())
        .start(ceProductMetricsStep)
        .build();
  }

  @Bean
  public Step ceProductMetricsStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("ceProductMetricsStep").tasklet(ceProductMetricsTasklet()).build();
  }
}
