/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config;

import io.harness.batch.processing.billing.tasklet.BillingDataGeneratedMailTasklet;
import io.harness.batch.processing.billing.writer.InstanceBillingAggregationDataTasklet;
import io.harness.batch.processing.billing.writer.InstanceBillingDataTasklet;
import io.harness.batch.processing.ccm.BatchJobType;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class BillingBatchConfiguration {
  @Autowired private StepBuilderFactory stepBuilderFactory;
  @Autowired private JobBuilderFactory jobBuilderFactory;

  @Bean
  public Tasklet instanceBillingDataTasklet() {
    return new InstanceBillingDataTasklet();
  }

  @Bean
  public Step instanceBillingStep() {
    return stepBuilderFactory.get("instanceBillingStep").tasklet(instanceBillingDataTasklet()).build();
  }

  @Bean
  public Tasklet billingDataGeneratedMailTasklet() {
    return new BillingDataGeneratedMailTasklet();
  }

  @Bean
  public Step billingDataGeneratedNotificationStep() {
    return stepBuilderFactory.get("billingDataGeneratedNotificationStep")
        .tasklet(billingDataGeneratedMailTasklet())
        .build();
  }

  @Bean
  @Qualifier(value = "instanceBillingHourlyJob")
  public Job instanceBillingHourlyJob(Step instanceBillingStep, Step billingDataGeneratedNotificationStep) {
    return jobBuilderFactory.get(BatchJobType.INSTANCE_BILLING_HOURLY.name())
        .incrementer(new RunIdIncrementer())
        .start(instanceBillingStep)
        .next(billingDataGeneratedNotificationStep)
        .build();
  }

  // ------------------------------------------------------------------------------------------

  @Bean
  @Qualifier(value = "instanceBillingJob")
  public Job instanceBillingJob(Step instanceBillingStep) {
    return jobBuilderFactory.get(BatchJobType.INSTANCE_BILLING.name())
        .incrementer(new RunIdIncrementer())
        .start(instanceBillingStep)
        .build();
  }

  // ------------------------------------------------------------------------------------------

  @Bean
  public Tasklet instanceBillingAggregationDataTasklet() {
    return new InstanceBillingAggregationDataTasklet();
  }

  @Bean
  public Step instanceBillingHourlyAggregationStep() {
    return stepBuilderFactory.get("instanceBillingHourlyAggregationStep")
        .tasklet(instanceBillingAggregationDataTasklet())
        .build();
  }

  @Bean
  @Qualifier(value = "instanceBillingHourlyAggregationJob")
  public Job instanceBillingHourlyAggregationJob(Step instanceBillingHourlyAggregationStep) {
    return jobBuilderFactory.get(BatchJobType.INSTANCE_BILLING_HOURLY_AGGREGATION.name())
        .incrementer(new RunIdIncrementer())
        .start(instanceBillingHourlyAggregationStep)
        .build();
  }

  @Bean
  public Step instanceBillingAggregationStep() {
    return stepBuilderFactory.get("instanceBillingAggregationStep")
        .tasklet(instanceBillingAggregationDataTasklet())
        .build();
  }

  @Bean
  @Qualifier(value = "instanceBillingAggregationJob")
  public Job instanceBillingAggregationJob(Step instanceBillingAggregationStep) {
    return jobBuilderFactory.get(BatchJobType.INSTANCE_BILLING_AGGREGATION.name())
        .incrementer(new RunIdIncrementer())
        .start(instanceBillingAggregationStep)
        .build();
  }
}
