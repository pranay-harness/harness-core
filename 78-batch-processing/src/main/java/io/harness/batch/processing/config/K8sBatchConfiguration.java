package io.harness.batch.processing.config;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.reader.EventReaderFactory;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class K8sBatchConfiguration {
  private static final int BATCH_SIZE = 10;
  private static final int RETRY_LIMIT = 1;

  @Autowired @Qualifier("mongoEventReader") private EventReaderFactory eventReaderFactory;

  @Autowired @Qualifier("k8sNodeInfoProcessor") private ItemProcessor k8sNodeInfoProcessor;
  @Autowired @Qualifier("k8sNodeEventProcessor") private ItemProcessor k8sNodeEventProcessor;
  @Autowired @Qualifier("k8sPodInfoProcessor") private ItemProcessor k8sPodInfoProcessor;
  @Autowired @Qualifier("k8sPodEventProcessor") private ItemProcessor k8sPodEventProcessor;

  @Autowired @Qualifier("instanceInfoWriter") private ItemWriter instanceInfoWriter;
  @Autowired @Qualifier("instanceEventWriter") private ItemWriter instanceEventWriter;

  @Autowired private StepBuilderFactory stepBuilderFactory;
  @Autowired private JobBuilderFactory jobBuilderFactory;

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> k8sPodInfoMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    String messageType = EventTypeConstants.K8S_POD_INFO;
    return eventReaderFactory.getEventReader(messageType, startDate, endDate);
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> k8sPodEventMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    String messageType = EventTypeConstants.K8S_POD_EVENT;
    return eventReaderFactory.getEventReader(messageType, startDate, endDate);
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> k8sNodeInfoMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    String messageType = EventTypeConstants.K8S_NODE_INFO;
    return eventReaderFactory.getEventReader(messageType, startDate, endDate);
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> k8sNodeEventMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    String messageType = EventTypeConstants.K8S_NODE_EVENT;
    return eventReaderFactory.getEventReader(messageType, startDate, endDate);
  }

  @Bean
  @Qualifier(value = "k8sJob")
  public Job k8sJob(Step k8sNodeInfoStep, Step k8sNodeEventStep, Step k8sPodInfoStep, Step k8sPodEventStep) {
    return jobBuilderFactory.get("k8sJob")
        .incrementer(new RunIdIncrementer())
        .start(k8sNodeInfoStep)
        .next(k8sNodeEventStep)
        .next(k8sPodInfoStep)
        .next(k8sPodEventStep)
        .build();
  }

  @Bean
  public Step k8sNodeInfoStep() {
    return stepBuilderFactory.get("k8sNodeInfoStep")
        .<PublishedMessage, InstanceInfo>chunk(BATCH_SIZE)
        .reader(k8sNodeInfoMessageReader(null, null))
        .processor(k8sNodeInfoProcessor)
        .writer(instanceInfoWriter)
        .build();
  }

  @Bean
  public Step k8sNodeEventStep() {
    return stepBuilderFactory.get("k8sNodeEventStep")
        .<PublishedMessage, InstanceEvent>chunk(BATCH_SIZE)
        .reader(k8sNodeEventMessageReader(null, null))
        .processor(k8sNodeEventProcessor)
        .writer(instanceEventWriter)
        .build();
  }

  @Bean
  public Step k8sPodInfoStep() {
    return stepBuilderFactory.get("k8sPodInfoStep")
        .<PublishedMessage, InstanceInfo>chunk(BATCH_SIZE)
        .reader(k8sPodInfoMessageReader(null, null))
        .processor(k8sPodInfoProcessor)
        .writer(instanceInfoWriter)
        .build();
  }

  @Bean
  public Step k8sPodEventStep() {
    return stepBuilderFactory.get("k8sPodEventStep")
        .<PublishedMessage, InstanceEvent>chunk(BATCH_SIZE)
        .reader(k8sPodEventMessageReader(null, null))
        .processor(k8sPodEventProcessor)
        .writer(instanceEventWriter)
        .build();
  }
}
