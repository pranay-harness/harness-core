package io.harness.batch.processing.config;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.svcmetrics.BatchJobExecutionListener;
import io.harness.batch.processing.tasklet.RerunJobTasklet;
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
public class RerunJobBatchConfig {
  @Autowired private MetricService metricService;

  @Bean
  public Tasklet rerunJobTasklet() {
    return new RerunJobTasklet();
  }

  @Bean
  public Step rerunJobStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("rerunJobStep").tasklet(rerunJobTasklet()).build();
  }

  @Bean
  @Qualifier(value = "rerunJobDataJob")
  public Job rerunJobDataJob(JobBuilderFactory jobBuilderFactory, Step rerunJobStep) {
    return jobBuilderFactory.get(BatchJobType.RERUN_JOB.name())
        .incrementer(new RunIdIncrementer())
        .listener(new BatchJobExecutionListener(metricService))
        .start(rerunJobStep)
        .build();
  }
}
