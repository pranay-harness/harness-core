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
  @Qualifier(value = "ecsJob")
  public Job ecsEventJob(Step ec2InstanceInfoStep, Step ec2InstanceLifecycleStep) {
    return jobBuilderFactory.get("ecsEventJob")
        .incrementer(new RunIdIncrementer())
        .start(ec2InstanceInfoStep)
        .next(ec2InstanceLifecycleStep)
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
}
