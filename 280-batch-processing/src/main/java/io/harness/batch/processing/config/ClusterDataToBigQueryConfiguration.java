package io.harness.batch.processing.config;

import io.harness.batch.processing.billing.tasklet.BillingDataGeneratedMailTasklet;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.svcmetrics.BatchJobExecutionListener;
import io.harness.batch.processing.tasklet.ClusterDataToBigQueryTasklet;
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

@Slf4j
@Configuration
public class ClusterDataToBigQueryConfiguration {
  @Autowired private MetricService metricService;

  @Bean
  public Tasklet clusterDataToBigQueryTasklet() {
    return new ClusterDataToBigQueryTasklet();
  }

  @Bean
  public Tasklet billingDataGeneratedMailTasklet() {
    return new BillingDataGeneratedMailTasklet();
  }

  @Bean
  public Step billingDataGeneratedNotificationStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("billingDataGeneratedNotificationStep")
        .tasklet(billingDataGeneratedMailTasklet())
        .build();
  }

  @Bean
  @Autowired
  @Qualifier(value = "clusterDataToBigQueryJob")
  public Job clusterDataToBigQueryJob(JobBuilderFactory jobBuilderFactory, Step clusterDataToBigQueryStep) {
    return jobBuilderFactory.get(BatchJobType.CLUSTER_DATA_TO_BIG_QUERY.name())
        .incrementer(new RunIdIncrementer())
        .listener(new BatchJobExecutionListener(metricService))
        .start(clusterDataToBigQueryStep)
        .build();
  }

  @Bean
  public Step clusterDataToBigQueryStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("clusterDataToBigQueryStep").tasklet(clusterDataToBigQueryTasklet()).build();
  }

  // ------------------------------------------------------------------------------------------

  @Bean
  public Tasklet clusterDataHourlyToBigQueryTasklet() {
    return new ClusterDataToBigQueryTasklet();
  }

  @Bean
  @Autowired
  @Qualifier(value = "clusterDataHourlyToBigQueryJob")
  public Job clusterDataHourlyToBigQueryJob(JobBuilderFactory jobBuilderFactory, Step clusterDataHourlyToBigQueryStep,
      Step billingDataGeneratedNotificationStep) {
    return jobBuilderFactory.get(BatchJobType.CLUSTER_DATA_HOURLY_TO_BIG_QUERY.name())
        .incrementer(new RunIdIncrementer())
        .listener(new BatchJobExecutionListener(metricService))
        .start(clusterDataHourlyToBigQueryStep)
        .next(billingDataGeneratedNotificationStep)
        .build();
  }

  @Bean
  public Step clusterDataHourlyToBigQueryStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("clusterDataHourlyToBigQueryStep")
        .tasklet(clusterDataHourlyToBigQueryTasklet())
        .build();
  }
}
