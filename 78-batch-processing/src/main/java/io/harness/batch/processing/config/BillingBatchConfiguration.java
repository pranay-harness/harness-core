package io.harness.batch.processing.config;

import io.harness.batch.processing.billing.reader.InstanceDataEventReader;
import io.harness.batch.processing.billing.reader.InstanceDataMongoEventReader;
import io.harness.batch.processing.billing.writer.InstanceBillingDataWriter;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.entities.InstanceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class BillingBatchConfiguration {
  private static final int BATCH_SIZE = 100;

  @Bean
  public ItemWriter<InstanceData> instanceBillingDataWriter() {
    return new InstanceBillingDataWriter();
  }

  @Bean
  public InstanceDataEventReader instanceDataMongoEventReader() {
    return new InstanceDataMongoEventReader();
  }

  @Bean
  @StepScope
  public ItemReader<InstanceData> instanceInfoMessageReader(
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    try {
      return instanceDataMongoEventReader().getEventReader(startDate, endDate);
    } catch (Exception ex) {
      logger.error("Exception instanceInfoMessageReader ", ex);
      return null;
    }
  }

  @Bean
  @Qualifier(value = "instanceBillingJob")
  public Job instanceBillingJob(JobBuilderFactory jobBuilderFactory, Step instanceBillingStep) {
    return jobBuilderFactory.get(BatchJobType.INSTANCE_BILLING.name())
        .incrementer(new RunIdIncrementer())
        .start(instanceBillingStep)
        .build();
  }

  @Bean
  public Step instanceBillingStep(StepBuilderFactory stepBuilderFactory,
      ItemReader<InstanceData> instanceInfoMessageReader, ItemWriter<InstanceData> instanceBillingDataWriter) {
    return stepBuilderFactory.get("instanceBillingStep")
        .<InstanceData, InstanceData>chunk(BATCH_SIZE)
        .reader(instanceInfoMessageReader)
        .writer(instanceBillingDataWriter)
        .build();
  }
}
