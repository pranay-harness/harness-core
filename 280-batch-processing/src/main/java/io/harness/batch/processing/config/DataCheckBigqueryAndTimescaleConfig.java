package io.harness.batch.processing.config;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.svcmetrics.BatchJobExecutionListener;
import io.harness.batch.processing.tasklet.DataCheckBigqueryAndTimescaleTasklet;
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

@OwnedBy(HarnessTeam.CE)
@Slf4j
@Configuration
public class DataCheckBigqueryAndTimescaleConfig {
  @Autowired private MetricService metricService;

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
        .listener(new BatchJobExecutionListener(metricService))
        .start(dataCheckBigqueryAndTimescaleStep)
        .build();
  }
}
