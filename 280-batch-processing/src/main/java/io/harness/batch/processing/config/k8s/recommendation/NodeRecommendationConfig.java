package io.harness.batch.processing.config.k8s.recommendation;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.svcmetrics.BatchJobExecutionListener;
import io.harness.batch.processing.tasklet.K8sNodeRecommendationTasklet;
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
public class NodeRecommendationConfig {
  @Autowired private MetricService metricService;

  @Bean
  public Tasklet K8sNodeRecommendationTasklet() {
    return new K8sNodeRecommendationTasklet();
  }

  @Bean
  public Step k8sNodeRecommendationStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("k8sNodeRecommendationStep").tasklet(K8sNodeRecommendationTasklet()).build();
  }

  @Bean
  @Autowired
  @Qualifier(value = "k8sNodeRecommendationJob")
  public Job k8sNodeRecommendationJob(JobBuilderFactory jobBuilderFactory, Step k8sNodeRecommendationStep) {
    return jobBuilderFactory.get(BatchJobType.K8S_NODE_RECOMMENDATION.name())
        .incrementer(new RunIdIncrementer())
        .listener(new BatchJobExecutionListener(metricService))
        .start(k8sNodeRecommendationStep)
        .build();
  }
}
