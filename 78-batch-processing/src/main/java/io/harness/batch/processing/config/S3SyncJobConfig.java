package io.harness.batch.processing.config;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.reader.EventReaderFactory;
import io.harness.batch.processing.reader.S3SyncReader;
import io.harness.batch.processing.writer.S3SyncEventWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.wings.beans.SettingAttribute;

@Slf4j
@Configuration
public class S3SyncJobConfig {
  private static final int BATCH_SIZE = 10;

  @Autowired private JobBuilderFactory jobBuilderFactory;

  @Autowired private StepBuilderFactory stepBuilderFactory;

  @Autowired @Qualifier("mongoEventReader") private EventReaderFactory mongoEventReader;

  @Bean
  @Qualifier(value = "s3SyncJob")
  public Job s3SyncJob(JobBuilderFactory jobBuilderFactory, Step s3SyncStep) {
    return jobBuilderFactory.get(BatchJobType.SYNC_BILLING_REPORT_S3.name())
        .incrementer(new RunIdIncrementer())
        .start(s3SyncStep)
        .build();
  }

  @Bean
  public Step s3SyncStep(
      EventReaderFactory mongoEventReader, StepBuilderFactory stepBuilderFactory, S3SyncReader s3SyncReader) {
    return stepBuilderFactory.get("s3SyncStep")
        .<SettingAttribute, SettingAttribute>chunk(BATCH_SIZE)
        .reader(s3SyncReader)
        .writer(s3SyncWriter())
        .build();
  }

  @Bean
  public ItemWriter<SettingAttribute> s3SyncWriter() {
    return new S3SyncEventWriter();
  }
}
