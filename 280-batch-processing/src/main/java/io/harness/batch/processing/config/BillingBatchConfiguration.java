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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class BillingBatchConfiguration {
  @Bean
  public Tasklet billingDataGeneratedMailTasklet() {
    return new BillingDataGeneratedMailTasklet();
  }

  @Bean
  public Tasklet instanceBillingDataTasklet() {
    return new InstanceBillingDataTasklet();
  }

  @Bean
  public Tasklet instanceBillingAggregationDataTasklet() {
    return new InstanceBillingAggregationDataTasklet();
  }

  @Bean
  @Qualifier(value = "instanceBillingJob")
  public Job instanceBillingJob(JobBuilderFactory jobBuilderFactory, Step instanceBillingStep) {
    return jobBuilderFactory.get(BatchJobType.INSTANCE_BILLING.name())
        .incrementer(new RunIdIncrementer())
        .start(instanceBillingStep)
        .build();
  }

  @Bean
  @Qualifier(value = "instanceBillingAggregationJob")
  public Job instanceBillingAggregationJob(JobBuilderFactory jobBuilderFactory, Step instanceBillingAggregationStep) {
    return jobBuilderFactory.get(BatchJobType.INSTANCE_BILLING_AGGREGATION.name())
        .incrementer(new RunIdIncrementer())
        .start(instanceBillingAggregationStep)
        .build();
  }

  @Bean
  @Qualifier(value = "instanceBillingHourlyJob")
  public Job instanceBillingHourlyJob(
      JobBuilderFactory jobBuilderFactory, Step instanceBillingStep, Step billingDataGeneratedNotificationStep) {
    return jobBuilderFactory.get(BatchJobType.INSTANCE_BILLING_HOURLY.name())
        .incrementer(new RunIdIncrementer())
        .start(instanceBillingStep)
        .next(billingDataGeneratedNotificationStep)
        .build();
  }

  @Bean
  public Step billingDataGeneratedNotificationStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("billingDataGeneratedNotificationStep")
        .tasklet(billingDataGeneratedMailTasklet())
        .build();
  }

  @Bean
  public Step instanceBillingStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("instanceBillingStep").tasklet(instanceBillingDataTasklet()).build();
  }

  @Bean
  public Step instanceBillingAggregationStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("instanceBillingAggregationStep")
        .tasklet(instanceBillingAggregationDataTasklet())
        .build();
  }
}
