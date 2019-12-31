package io.harness.batch.processing.config;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.reader.EventReaderFactory;
import io.harness.batch.processing.reader.K8sGranularUtilizationMetricsReader;
import io.harness.batch.processing.writer.K8sUtilizationMetricsWriter;
import io.harness.batch.processing.writer.NodeUtilizationMetricsWriter;
import io.harness.batch.processing.writer.PodUtilizationMetricsWriter;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class K8sUtilizationConfiguration {
  private static final int BATCH_SIZE = 50;
  private static final int GRANULAR_BATCH_SIZE = 1000;

  @Autowired @Qualifier("mongoEventReader") private EventReaderFactory eventReaderFactory;
  @Autowired private StepBuilderFactory stepBuilderFactory;

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> k8sNodeUtilizationEventMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    return eventReaderFactory.getEventReader(EventTypeConstants.NODE_UTILIZATION, startDate, endDate);
  }

  @Bean
  public ItemReader<List<String>> k8sUtilizationAggregationReader() {
    return new K8sGranularUtilizationMetricsReader();
  }

  @Bean
  public ItemWriter<PublishedMessage> nodeUtilizationMetricsWriter() {
    return new NodeUtilizationMetricsWriter();
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> k8sPodUtilizationEventMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    return eventReaderFactory.getEventReader(EventTypeConstants.POD_UTILIZATION, startDate, endDate);
  }

  @Bean
  public ItemWriter<PublishedMessage> podUtilizationMetricsWriter() {
    return new PodUtilizationMetricsWriter();
  }

  @Bean
  public ItemWriter<List<String>> k8sUtilizationAggregationWriter() {
    return new K8sUtilizationMetricsWriter();
  }

  @Bean
  public Step k8sPodUtilizationEventStep() {
    return stepBuilderFactory.get("k8sPodUtilizationEventStep")
        .<PublishedMessage, PublishedMessage>chunk(GRANULAR_BATCH_SIZE)
        .reader(k8sPodUtilizationEventMessageReader(null, null))
        .writer(podUtilizationMetricsWriter())
        .build();
  }

  @Bean
  public Step k8sNodeUtilizationEventStep() {
    return stepBuilderFactory.get("k8sNodeUtilizationEventStep")
        .<PublishedMessage, PublishedMessage>chunk(GRANULAR_BATCH_SIZE)
        .reader(k8sNodeUtilizationEventMessageReader(null, null))
        .writer(nodeUtilizationMetricsWriter())
        .build();
  }

  @Bean
  public Step k8sUtilizationAggregationStep() {
    return stepBuilderFactory.get("k8sUtilizationAggregationStep")
        .<List<String>, List<String>>chunk(BATCH_SIZE)
        .reader(k8sUtilizationAggregationReader())
        .writer(k8sUtilizationAggregationWriter())
        .build();
  }

  @Bean
  @Autowired
  @Qualifier(value = "k8sUtilizationJob")
  public Job k8sUtilizationJob(JobBuilderFactory jobBuilderFactory, Step k8sPodUtilizationEventStep,
      Step k8sNodeUtilizationEventStep, Step k8sUtilizationAggregationStep) {
    return jobBuilderFactory.get(BatchJobType.K8S_UTILIZATION.name())
        .incrementer(new RunIdIncrementer())
        .start(k8sPodUtilizationEventStep)
        .next(k8sNodeUtilizationEventStep)
        .next(k8sUtilizationAggregationStep)
        .build();
  }
}