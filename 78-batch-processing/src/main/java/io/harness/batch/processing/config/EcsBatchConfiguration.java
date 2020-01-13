package io.harness.batch.processing.config;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.reader.EventReaderFactory;
import io.harness.batch.processing.writer.Ec2InstanceInfoWriter;
import io.harness.batch.processing.writer.Ec2InstanceLifecycleWriter;
import io.harness.batch.processing.writer.EcsContainerInstanceInfoWriter;
import io.harness.batch.processing.writer.EcsContainerInstanceLifecycleWriter;
import io.harness.batch.processing.writer.EcsSyncEventWriter;
import io.harness.batch.processing.writer.EcsTaskInfoWriter;
import io.harness.batch.processing.writer.EcsTaskLifecycleWriter;
import io.harness.batch.processing.writer.EcsUtilizationMetricsWriter;
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

@Slf4j
@Configuration
public class EcsBatchConfiguration {
  private static final int BATCH_SIZE = 500;
  private static final int SKIP_BATCH_SIZE = 5;
  private static final int RETRY_LIMIT = 1;

  @Autowired private JobBuilderFactory jobBuilderFactory;

  @Autowired private StepBuilderFactory stepBuilderFactory;

  @Autowired private SkipListener ecsStepSkipListener;

  @Autowired @Qualifier("mongoEventReader") private EventReaderFactory mongoEventReader;

  @Bean
  public ItemWriter<PublishedMessage> ecsContainerInstanceLifecycleWriter() {
    return new EcsContainerInstanceLifecycleWriter();
  }

  @Bean
  public ItemWriter<PublishedMessage> ec2InstanceInfoWriter() {
    return new Ec2InstanceInfoWriter();
  }

  @Bean
  public ItemWriter<PublishedMessage> ec2InstanceLifecycleWriter() {
    return new Ec2InstanceLifecycleWriter();
  }

  @Bean
  public ItemWriter<PublishedMessage> ecsContainerInstanceInfoWriter() {
    return new EcsContainerInstanceInfoWriter();
  }

  @Bean
  public ItemWriter<PublishedMessage> ecsTaskInfoWriter() {
    return new EcsTaskInfoWriter();
  }

  @Bean
  public ItemWriter<PublishedMessage> ecsUtilizationMetricsWriter() {
    return new EcsUtilizationMetricsWriter();
  }

  @Bean
  public ItemWriter<PublishedMessage> ecsTaskLifecycleWriter() {
    return new EcsTaskLifecycleWriter();
  }

  @Bean
  public ItemWriter<PublishedMessage> ecsSyncEventWriter() {
    return new EcsSyncEventWriter();
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ec2InstanceInfoMessageReader(
      @Value("#{jobParameters[accountId]}") String accountId, @Value("#{jobParameters[startDate]}") Long startDate,
      @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      return mongoEventReader.getEventReader(accountId, EventTypeConstants.EC2_INSTANCE_INFO, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ec2InstanceInfoMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ec2InstanceLifecycleMessageReader(
      @Value("#{jobParameters[accountId]}") String accountId, @Value("#{jobParameters[startDate]}") Long startDate,
      @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      return mongoEventReader.getEventReader(accountId, EventTypeConstants.EC2_INSTANCE_LIFECYCLE, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ec2InstanceInfoMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ecsContainerInstanceInfoMessageReader(
      @Value("#{jobParameters[accountId]}") String accountId, @Value("#{jobParameters[startDate]}") Long startDate,
      @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      return mongoEventReader.getEventReader(
          accountId, EventTypeConstants.ECS_CONTAINER_INSTANCE_INFO, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ecsContainerInstanceInfoMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ecsContainerInstanceLifecycleMessageReader(
      @Value("#{jobParameters[accountId]}") String accountId, @Value("#{jobParameters[startDate]}") Long startDate,
      @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      return mongoEventReader.getEventReader(
          accountId, EventTypeConstants.ECS_CONTAINER_INSTANCE_LIFECYCLE, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ecsContainerInstanceLifecycleMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ecsTaskInfoMessageReader(@Value("#{jobParameters[accountId]}") String accountId,
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      return mongoEventReader.getEventReader(accountId, EventTypeConstants.ECS_TASK_INFO, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ecsTaskInfoMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ecsSyncEventMessageReader(@Value("#{jobParameters[accountId]}") String accountId,
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      return mongoEventReader.getEventReader(accountId, EventTypeConstants.ECS_SYNC_EVENT, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ecsSyncEventMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ecsTaskLifecycleMessageReader(
      @Value("#{jobParameters[accountId]}") String accountId, @Value("#{jobParameters[startDate]}") Long startDate,
      @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      return mongoEventReader.getEventReader(accountId, EventTypeConstants.ECS_TASK_LIFECYCLE, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ecsTaskLifecycleMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> ecsUtilizationMetricsMessageReader(
      @Value("#{jobParameters[accountId]}") String accountId, @Value("#{jobParameters[startDate]}") Long startDate,
      @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      return mongoEventReader.getEventReader(accountId, EventTypeConstants.ECS_UTILIZATION, startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception ecsUtilizationMetricsMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @Qualifier(value = "ecsJob")
  public Job ecsEventJob(Step ec2InstanceInfoStep, Step ec2InstanceLifecycleStep, Step ecsContainerInstanceInfoStep,
      Step ecsContainerInstanceLifecycleStep, Step ecsTaskInfoStep, Step ecsTaskLifecycleStep, Step ecsSyncEventStep) {
    return jobBuilderFactory.get(BatchJobType.ECS_EVENT.name())
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
  @Qualifier(value = "ecsUtilizationJob")
  public Job ecsUtilizationJob(Step ecsUtilizationMetricsStep) {
    return jobBuilderFactory.get(BatchJobType.ECS_UTILIZATION.name())
        .incrementer(new RunIdIncrementer())
        .start(ecsUtilizationMetricsStep)
        .build();
  }

  @Bean
  public Step ec2InstanceInfoStep() {
    return stepBuilderFactory.get("ec2InstanceInfoStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .reader(ec2InstanceInfoMessageReader(null, null, null))
        .writer(ec2InstanceInfoWriter())
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
        .reader(ec2InstanceLifecycleMessageReader(null, null, null))
        .writer(ec2InstanceLifecycleWriter())
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
        .reader(ecsContainerInstanceInfoMessageReader(null, null, null))
        .writer(ecsContainerInstanceInfoWriter())
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
        .reader(ecsContainerInstanceLifecycleMessageReader(null, null, null))
        .writer(ecsContainerInstanceLifecycleWriter())
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
        .reader(ecsTaskInfoMessageReader(null, null, null))
        .writer(ecsTaskInfoWriter())
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
        .reader(ecsTaskLifecycleMessageReader(null, null, null))
        .writer(ecsTaskLifecycleWriter())
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
        .reader(ecsSyncEventMessageReader(null, null, null))
        .writer(ecsSyncEventWriter())
        .faultTolerant()
        .retryLimit(RETRY_LIMIT)
        .retry(Exception.class)
        .skipLimit(SKIP_BATCH_SIZE)
        .skip(Exception.class)
        .listener(ecsStepSkipListener)
        .build();
  }

  @Bean
  public Step ecsUtilizationMetricsStep() {
    return stepBuilderFactory.get("ecsUtilizationMetricsStep")
        .<PublishedMessage, PublishedMessage>chunk(BATCH_SIZE)
        .reader(ecsUtilizationMetricsMessageReader(null, null, null))
        .writer(ecsUtilizationMetricsWriter())
        .faultTolerant()
        .retryLimit(RETRY_LIMIT)
        .retry(Exception.class)
        .skipLimit(SKIP_BATCH_SIZE)
        .skip(Exception.class)
        .listener(ecsStepSkipListener)
        .build();
  }
}
