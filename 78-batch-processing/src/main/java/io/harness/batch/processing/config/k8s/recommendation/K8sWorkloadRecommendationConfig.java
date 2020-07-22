package io.harness.batch.processing.config.k8s.recommendation;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.reader.EventReaderFactory;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class K8sWorkloadRecommendationConfig {
  private static final int BATCH_SIZE = 1000;

  private final EventReaderFactory eventReaderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  public K8sWorkloadRecommendationConfig(
      @Qualifier("mongoEventReader") EventReaderFactory eventReaderFactory, StepBuilderFactory stepBuilderFactory) {
    this.eventReaderFactory = eventReaderFactory;
    this.stepBuilderFactory = stepBuilderFactory;
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> containerStateReader(@Value("#{jobParameters[accountId]}") String accountId,
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    return eventReaderFactory.getEventReader(accountId, EventTypeConstants.K8S_CONTAINER_STATE, startDate, endDate);
  }

  @Bean
  public ItemProcessor<PublishedMessage, PublishedMessage> passThroughItemProcessor() {
    return new PassThroughItemProcessor<>();
  }

  @Bean
  public Step containerStateStep(ItemReader<? extends PublishedMessage> containerStateReader,
      ItemProcessor<? super PublishedMessage, ? extends PublishedMessage> passThroughItemProcessor,
      ContainerStateWriter containerStateWriter, SkipListener<PublishedMessage, PublishedMessage> skipListener) {
    return stepBuilderFactory.get("k8sRecommenderStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .faultTolerant()
        .retry(Exception.class)
        .retryLimit(1)
        .skip(Exception.class)
        .skipLimit(50)
        .listener(skipListener)
        .reader(containerStateReader)
        .processor(passThroughItemProcessor)
        .writer(containerStateWriter)
        .build();
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> workloadSpecReader(@Value("#{jobParameters[accountId]}") String accountId,
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    return eventReaderFactory.getEventReader(accountId, EventTypeConstants.K8S_WORKLOAD_SPEC, startDate, endDate);
  }

  @Bean
  public Step workloadSpecStep(ItemReader<? extends PublishedMessage> workloadSpecReader,
      ItemProcessor<? super PublishedMessage, ? extends PublishedMessage> passThroughItemProcessor,
      WorkloadSpecWriter workloadSpecWriter, SkipListener<PublishedMessage, PublishedMessage> skipListener) {
    return stepBuilderFactory.get("workloadSpecStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .faultTolerant()
        .retry(Exception.class)
        .retryLimit(1)
        .skip(Exception.class)
        .skipLimit(50)
        .listener(skipListener)
        .reader(workloadSpecReader)
        .processor(passThroughItemProcessor)
        .writer(workloadSpecWriter)
        .build();
  }

  @Bean
  public Job k8sRecommendationJob(JobBuilderFactory jobBuilderFactory, Step containerStateStep, Step workloadSpecStep) {
    return jobBuilderFactory.get(BatchJobType.K8S_WORKLOAD_RECOMMENDATION.name())
        .incrementer(new RunIdIncrementer())
        .start(workloadSpecStep)
        .next(containerStateStep)
        .build();
  }
}
