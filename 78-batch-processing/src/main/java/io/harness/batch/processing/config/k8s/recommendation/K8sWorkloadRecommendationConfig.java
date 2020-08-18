package io.harness.batch.processing.config.k8s.recommendation;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.reader.CloseableIteratorItemReader;
import io.harness.batch.processing.reader.EventReaderFactory;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation.K8sWorkloadRecommendationKeys;

import java.time.Instant;
import java.util.Iterator;

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
  public ItemReader<PublishedMessage> workloadSpecReader(@Value("#{jobParameters[accountId]}") String accountId,
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    return eventReaderFactory.getEventReader(accountId, EventTypeConstants.K8S_WORKLOAD_SPEC, startDate, endDate);
  }

  @Bean
  public Step workloadSpecStep(ItemReader<? extends PublishedMessage> workloadSpecReader,
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
        .processor(new PassThroughItemProcessor<>())
        .writer(workloadSpecWriter)
        .build();
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> containerStateReader(@Value("#{jobParameters[accountId]}") String accountId,
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    return eventReaderFactory.getEventReader(accountId, EventTypeConstants.K8S_CONTAINER_STATE, startDate, endDate);
  }

  @Bean
  public Step containerStateStep(ItemReader<? extends PublishedMessage> containerStateReader,
      ContainerStateWriter containerStateWriter, SkipListener<PublishedMessage, PublishedMessage> skipListener) {
    return stepBuilderFactory.get("containerStateStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .faultTolerant()
        .retry(Exception.class)
        .retryLimit(1)
        .skip(Exception.class)
        .skipLimit(50)
        .listener(skipListener)
        .reader(containerStateReader)
        .processor(new PassThroughItemProcessor<>())
        .writer(containerStateWriter)
        .build();
  }

  @Bean
  @StepScope
  public ItemReader<K8sWorkloadRecommendation> dirtyRecommendationReader(
      @Value("#{jobParameters[accountId]}") String accountId, HPersistence hPersistence, MongoTemplate mongoTemplate) {
    Iterator<K8sWorkloadRecommendation> hIterator =
        new HIterator<>(hPersistence.createQuery(K8sWorkloadRecommendation.class)
                            .field(K8sWorkloadRecommendationKeys.accountId)
                            .equal(accountId)
                            .field(K8sWorkloadRecommendationKeys.dirty)
                            .equal(Boolean.TRUE)
                            .fetch());
    return new CloseableIteratorItemReader<>(hIterator);
  }

  @Bean
  @StepScope
  public ComputedRecommendationWriter computedRecommendationWriter(WorkloadRecommendationDao workloadRecommendationDao,
      WorkloadCostService workloadCostService, @Value("#{jobParameters[startDate]}") Long startDateMillis) {
    Instant jobStartDate = Instant.ofEpochMilli(startDateMillis);
    return new ComputedRecommendationWriter(workloadRecommendationDao, workloadCostService, jobStartDate);
  }

  @Bean
  public Step computeRecommendationStep(ItemReader<K8sWorkloadRecommendation> dirtyRecommendationReader,
      ItemProcessor<K8sWorkloadRecommendation, K8sWorkloadRecommendation> passThroughItemProcessor,
      ComputedRecommendationWriter computedRecommendationWriter,
      SkipListener<K8sWorkloadRecommendation, K8sWorkloadRecommendation> skipListener) {
    return stepBuilderFactory.get("computeRecommendationStep")
        .<K8sWorkloadRecommendation, K8sWorkloadRecommendation>chunk(BATCH_SIZE)
        .faultTolerant()
        .retry(Exception.class)
        .retryLimit(1)
        .skip(Exception.class)
        .skipLimit(50)
        .listener(skipListener)
        .reader(dirtyRecommendationReader)
        .processor(new PassThroughItemProcessor<>())
        .writer(computedRecommendationWriter)
        .build();
  }

  @Bean
  public Job k8sRecommendationJob(JobBuilderFactory jobBuilderFactory, Step containerStateStep, Step workloadSpecStep,
      Step computeRecommendationStep) {
    return jobBuilderFactory.get(BatchJobType.K8S_WORKLOAD_RECOMMENDATION.name())
        .incrementer(new RunIdIncrementer())
        // process WorkloadSpec messages and update current requests & limits.
        .start(workloadSpecStep)
        // process ContainerState messages and update histograms.
        .next(containerStateStep)
        // recompute recommendations if updated in last 2 steps.
        .next(computeRecommendationStep)
        .build();
  }
}
