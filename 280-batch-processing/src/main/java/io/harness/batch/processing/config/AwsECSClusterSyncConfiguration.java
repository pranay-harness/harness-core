/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.AwsECSClusterSyncTasklet;

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

@Slf4j
@Configuration
public class AwsECSClusterSyncConfiguration {
  @Bean
  public Tasklet awsECSClusterSyncTasklet() {
    return new AwsECSClusterSyncTasklet();
  }

  @Bean
  @Autowired
  @Qualifier(value = "awsECSClusterSyncJob")
  public Job awsECSClusterSyncJob(JobBuilderFactory jobBuilderFactory, Step awsECSClusterSyncStep) {
    return jobBuilderFactory.get(BatchJobType.AWS_ECS_CLUSTER_SYNC.name())
        .incrementer(new RunIdIncrementer())
        .start(awsECSClusterSyncStep)
        .build();
  }

  @Bean
  public Step awsECSClusterSyncStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("awsECSClusterSyncStep").tasklet(awsECSClusterSyncTasklet()).build();
  }
}
