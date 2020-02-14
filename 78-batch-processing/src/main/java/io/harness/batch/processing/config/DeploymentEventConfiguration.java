package io.harness.batch.processing.config;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.events.deployment.writer.DeploymentEventWriter;
import io.harness.batch.processing.reader.DeploymentEventReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class DeploymentEventConfiguration {
  private static final int DEPLOYMENT_BATCH_SIZE = 10;

  @Bean
  public ItemWriter<List<String>> deploymentEventWriter() {
    return new DeploymentEventWriter();
  }

  @Bean
  public Step deploymentEventStep(StepBuilderFactory stepBuilderFactory, DeploymentEventReader deploymentEventReader) {
    return stepBuilderFactory.get("deploymentEventStep")
        .<List<String>, List<String>>chunk(DEPLOYMENT_BATCH_SIZE)
        .reader(deploymentEventReader)
        .writer(deploymentEventWriter())
        .build();
  }

  @Bean
  @Autowired
  @Qualifier(value = "deploymentEventJob")
  public Job deploymentEventJob(JobBuilderFactory jobBuilderFactory, Step deploymentEventStep) {
    return jobBuilderFactory.get(BatchJobType.DEPLOYMENT_EVENT.name())
        .incrementer(new RunIdIncrementer())
        .start(deploymentEventStep)
        .build();
  }
}