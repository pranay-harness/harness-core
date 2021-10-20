package io.harness.batch.processing.config;

import io.harness.batch.processing.billing.writer.InstanceBillingAggregationDataTasklet;
import io.harness.batch.processing.billing.writer.InstanceBillingDataTasklet;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.svcmetrics.BatchJobExecutionListener;
import io.harness.metrics.service.api.MetricService;

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
  @Autowired private MetricService metricService;

  @Bean
  public Tasklet instanceBillingDataTasklet() {
    return new InstanceBillingDataTasklet();
  }

  @Bean
  public Step instanceBillingStep() {
    return stepBuilderFactory.get("instanceBillingStep").tasklet(instanceBillingDataTasklet()).build();
  }

  @Bean
  @Qualifier(value = "instanceBillingHourlyJob")
  public Job instanceBillingHourlyJob(Step instanceBillingStep) {
    return jobBuilderFactory.get(BatchJobType.INSTANCE_BILLING_HOURLY.name())
        .incrementer(new RunIdIncrementer())
        .listener(new BatchJobExecutionListener(metricService))
        .start(instanceBillingStep)
        .build();
  }

  // ------------------------------------------------------------------------------------------

  @Bean
  @Qualifier(value = "instanceBillingJob")
  public Job instanceBillingJob(Step instanceBillingStep) {
    return jobBuilderFactory.get(BatchJobType.INSTANCE_BILLING.name())
        .incrementer(new RunIdIncrementer())
        .listener(new BatchJobExecutionListener(metricService))
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
        .listener(new BatchJobExecutionListener(metricService))
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
        .listener(new BatchJobExecutionListener(metricService))
        .start(instanceBillingAggregationStep)
        .build();
  }
}
