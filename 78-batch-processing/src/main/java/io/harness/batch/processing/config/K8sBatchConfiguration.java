package io.harness.batch.processing.config;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.processor.K8sNodeEventProcessor;
import io.harness.batch.processing.processor.K8sNodeInfoProcessor;
import io.harness.batch.processing.processor.K8sPodEventProcessor;
import io.harness.batch.processing.processor.K8sPodInfoProcessor;
import io.harness.batch.processing.reader.EventReaderFactory;
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

  @Autowired private StepBuilderFactory stepBuilderFactory;

  @Autowired @Qualifier("mongoEventReader") private EventReaderFactory eventReaderFactory;
  @Autowired @Qualifier("instanceInfoWriter") private ItemWriter instanceInfoWriter;
  @Autowired @Qualifier("instanceEventWriter") private ItemWriter instanceEventWriter;

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
  public ItemReader<PublishedMessage> k8sPodUtilizationEventMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    String messageType = EventTypeConstants.POD_UTILIZATION;
    return eventReaderFactory.getEventReader(messageType, startDate, endDate);
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> k8sNodeUtilizationEventMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    String messageType = EventTypeConstants.NODE_UTILIZATION;
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
  public ItemProcessor<PublishedMessage, InstanceInfo> k8sNodeInfoProcessor() {
    return new K8sNodeInfoProcessor();
  }

  @Bean
  public ItemProcessor<PublishedMessage, InstanceEvent> k8sNodeEventProcessor() {
    return new K8sNodeEventProcessor();
  }

  @Bean
  public ItemProcessor<PublishedMessage, InstanceInfo> k8sPodInfoProcessor() {
    return new K8sPodInfoProcessor();
  }

  @Bean
  public ItemProcessor<PublishedMessage, InstanceEvent> k8sPodEventProcessor() {
    return new K8sPodEventProcessor();
  }

  @Bean
  public ItemWriter<PublishedMessage> podUtilizationMetricsWriter() {
    return new PodUtilizationMetricsWriter();
  }

  @Bean
  public ItemWriter<PublishedMessage> nodeUtilizationMetricsWriter() {
    return new NodeUtilizationMetricsWriter();
  }

  @Bean
  @Autowired
  @Qualifier(value = "k8sJob")
  public Job k8sJob(JobBuilderFactory jobBuilderFactory, Step k8sNodeInfoStep, Step k8sNodeEventStep,
      Step k8sPodInfoStep, Step k8sPodEventStep) {
    return jobBuilderFactory.get("k8sJob")
        .incrementer(new RunIdIncrementer())
        .start(k8sNodeInfoStep)
        .next(k8sNodeEventStep)
        .next(k8sPodInfoStep)
        .next(k8sPodEventStep)
        .build();
  }

  @Bean
  @Autowired
  @Qualifier(value = "k8sUtilizationJob")
  public Job k8sUtilizationJob(
      JobBuilderFactory jobBuilderFactory, Step k8sPodUtilizationEventStep, Step k8sNodeUtilizationEventStep) {
    return jobBuilderFactory.get("k8sUtilizationJob")
        .incrementer(new RunIdIncrementer())
        .start(k8sPodUtilizationEventStep)
        .next(k8sNodeUtilizationEventStep)
        .build();
  }

  @Bean
  public Step k8sNodeInfoStep() {
    return stepBuilderFactory.get("k8sNodeInfoStep")
        .<PublishedMessage, InstanceInfo>chunk(BATCH_SIZE)
        .reader(k8sNodeInfoMessageReader(null, null))
        .processor(k8sNodeInfoProcessor())
        .writer(instanceInfoWriter)
        .build();
  }

  @Bean
  public Step k8sNodeEventStep() {
    return stepBuilderFactory.get("k8sNodeEventStep")
        .<PublishedMessage, InstanceEvent>chunk(BATCH_SIZE)
        .reader(k8sNodeEventMessageReader(null, null))
        .processor(k8sNodeEventProcessor())
        .writer(instanceEventWriter)
        .build();
  }

  @Bean
  public Step k8sPodInfoStep() {
    return stepBuilderFactory.get("k8sPodInfoStep")
        .<PublishedMessage, InstanceInfo>chunk(BATCH_SIZE)
        .reader(k8sPodInfoMessageReader(null, null))
        .processor(k8sPodInfoProcessor())
        .writer(instanceInfoWriter)
        .build();
  }

  @Bean
  public Step k8sPodEventStep() {
    return stepBuilderFactory.get("k8sPodEventStep")
        .<PublishedMessage, InstanceEvent>chunk(BATCH_SIZE)
        .reader(k8sPodEventMessageReader(null, null))
        .processor(k8sPodEventProcessor())
        .writer(instanceEventWriter)
        .build();
  }

  @Bean
  public Step k8sPodUtilizationEventStep() {
    return stepBuilderFactory.get("k8sPodUtilizationEventStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .reader(k8sPodUtilizationEventMessageReader(null, null))
        .writer(podUtilizationMetricsWriter())
        .build();
  }

  @Bean
  public Step k8sNodeUtilizationEventStep() {
    return stepBuilderFactory.get("k8sNodeUtilizationEventStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .reader(k8sNodeUtilizationEventMessageReader(null, null))
        .writer(nodeUtilizationMetricsWriter())
        .build();
  }
}
