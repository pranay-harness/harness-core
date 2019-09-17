package io.harness.batch.processing.config;

import io.harness.batch.processing.reader.EventReader;
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
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EcsBatchConfiguration {
  private static final int BATCH_SIZE = 10;
  private static final int SKIP_BATCH_SIZE = 5;
  private static final int RETRY_LIMIT = 1;

  @Autowired private JobBuilderFactory jobBuilderFactory;

  @Autowired private StepBuilderFactory stepBuilderFactory;

  @Autowired private SkipListener ecsStepSkipListener;

  @Autowired @Qualifier("ec2InstanceInfoWriter") private ItemWriter ec2InstanceInfoWriter;

  @Autowired @Qualifier("ec2InstanceLifecycleWriter") private ItemWriter ec2InstanceLifecycleWriter;

  @Autowired @Qualifier("ecsContainerInstanceInfoWriter") private ItemWriter ecsContainerInstanceInfoWriter;

  @Autowired @Qualifier("ecsContainerInstanceLifecycleWriter") private ItemWriter ecsContainerInstanceLifecycleWriter;

  @Autowired @Qualifier("ecsTaskInfoWriter") private ItemWriter ecsTaskInfoWriter;

  @Autowired @Qualifier("ecsTaskLifecycleWriter") private ItemWriter ecsTaskLifecycleWriter;

  @Autowired @Qualifier("ecsSyncEventWriter") private ItemWriter ecsSyncEventWriter;

  @Autowired @Qualifier("mongoEventReader") private EventReader mongoEventReader;

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ec2InstanceInfoMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      String messageType = EventTypeConstants.EC2_INSTANCE_INFO;
      return mongoEventReader.getEventReader(messageType, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ec2InstanceInfoMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ec2InstanceLifecycleMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      String messageType = EventTypeConstants.EC2_INSTANCE_LIFECYCLE;
      return mongoEventReader.getEventReader(messageType, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ec2InstanceInfoMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ecsContainerInstanceInfoMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      String messageType = EventTypeConstants.ECS_CONTAINER_INSTANCE_INFO;
      return mongoEventReader.getEventReader(messageType, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ecsContainerInstanceInfoMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ecsContainerInstanceLifecycleMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      String messageType = EventTypeConstants.ECS_CONTAINER_INSTANCE_LIFECYCLE;
      return mongoEventReader.getEventReader(messageType, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ecsContainerInstanceLifecycleMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ecsTaskInfoMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      String messageType = EventTypeConstants.ECS_TASK_INFO;
      return mongoEventReader.getEventReader(messageType, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ecsTaskInfoMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ecsSyncEventMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      String messageType = EventTypeConstants.ECS_SYNC_EVENT;
      return mongoEventReader.getEventReader(messageType, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ecsSyncEventMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ecsTaskLifecycleMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      String messageType = EventTypeConstants.ECS_TASK_LIFECYCLE;
      return mongoEventReader.getEventReader(messageType, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ecsTaskLifecycleMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @Qualifier(value = "ecsJob")
  public Job ecsEventJob(Step ec2InstanceInfoStep, Step ec2InstanceLifecycleStep, Step ecsContainerInstanceInfoStep,
      Step ecsContainerInstanceLifecycleStep, Step ecsTaskInfoStep, Step ecsTaskLifecycleStep, Step ecsSyncEventStep) {
    return jobBuilderFactory.get("ecsEventJob")
        .incrementer(new RunIdIncrementer())
        .start(ec2InstanceInfoStep)
        .next(ec2InstanceLifecycleStep)
        .next(ecsContainerInstanceInfoStep)
        .next(ecsContainerInstanceLifecycleStep)
        .next(ecsTaskInfoStep)
        .next(ecsTaskLifecycleStep)
        .next(ecsSyncEventStep)
        .build();
  }

  @Bean
  public Step ec2InstanceInfoStep() {
    return stepBuilderFactory.get("ec2InstanceInfoStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .reader(ec2InstanceInfoMessageReader(null, null))
        .writer(ec2InstanceInfoWriter)
        .faultTolerant()
        .retryLimit(RETRY_LIMIT)
        .retry(Exception.class)
        .skipLimit(SKIP_BATCH_SIZE)
        .skip(Exception.class)
        .listener(ecsStepSkipListener)
        .build();
  }

  @Bean
  public Step ec2InstanceLifecycleStep() {
    return stepBuilderFactory.get("ec2InstanceLifecycleStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .reader(ec2InstanceLifecycleMessageReader(null, null))
        .writer(ec2InstanceLifecycleWriter)
        .faultTolerant()
        .retryLimit(RETRY_LIMIT)
        .retry(Exception.class)
        .skipLimit(SKIP_BATCH_SIZE)
        .skip(Exception.class)
        .listener(ecsStepSkipListener)
        .build();
  }

  @Bean
  public Step ecsContainerInstanceInfoStep() {
    return stepBuilderFactory.get("ecsContainerInstanceInfoStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .reader(ecsContainerInstanceInfoMessageReader(null, null))
        .writer(ecsContainerInstanceInfoWriter)
        .faultTolerant()
        .retryLimit(RETRY_LIMIT)
        .retry(Exception.class)
        .skipLimit(SKIP_BATCH_SIZE)
        .skip(Exception.class)
        .listener(ecsStepSkipListener)
        .build();
  }

  @Bean
  public Step ecsContainerInstanceLifecycleStep() {
    return stepBuilderFactory.get("ecsContainerInstanceLifecycleStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .reader(ecsContainerInstanceLifecycleMessageReader(null, null))
        .writer(ecsContainerInstanceLifecycleWriter)
        .faultTolerant()
        .retryLimit(RETRY_LIMIT)
        .retry(Exception.class)
        .skipLimit(SKIP_BATCH_SIZE)
        .skip(Exception.class)
        .listener(ecsStepSkipListener)
        .build();
  }

  @Bean
  public Step ecsTaskInfoStep() {
    return stepBuilderFactory.get("ecsTaskInfoStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .reader(ecsTaskInfoMessageReader(null, null))
        .writer(ecsTaskInfoWriter)
        .faultTolerant()
        .retryLimit(RETRY_LIMIT)
        .retry(Exception.class)
        .skipLimit(SKIP_BATCH_SIZE)
        .skip(Exception.class)
        .listener(ecsStepSkipListener)
        .build();
  }

  @Bean
  public Step ecsTaskLifecycleStep() {
    return stepBuilderFactory.get("ecsTaskLifecycleStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .reader(ecsTaskLifecycleMessageReader(null, null))
        .writer(ecsTaskLifecycleWriter)
        .faultTolerant()
        .retryLimit(RETRY_LIMIT)
        .retry(Exception.class)
        .skipLimit(SKIP_BATCH_SIZE)
        .skip(Exception.class)
        .listener(ecsStepSkipListener)
        .build();
  }

  @Bean
  public Step ecsSyncEventStep() {
    return stepBuilderFactory.get("ecsSyncEventStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .reader(ecsSyncEventMessageReader(null, null))
        .writer(ecsSyncEventWriter)
        .faultTolerant()
        .retryLimit(RETRY_LIMIT)
        .retry(Exception.class)
        .skipLimit(SKIP_BATCH_SIZE)
        .skip(Exception.class)
        .listener(ecsStepSkipListener)
        .build();
  }
}
