package io.harness.batch.processing.config;

import io.harness.batch.processing.ccm.BatchJobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;

@Slf4j
// TODO(avmohan): Enable after adding implementations
//@Configuration
public class S3SyncJobConfig {
  private static final int BATCH_SIZE = 10;

  private final JobBuilderFactory jbf;
  private final StepBuilderFactory sbf;

  public S3SyncJobConfig(JobBuilderFactory jbf, StepBuilderFactory sbf) {
    this.jbf = jbf;
    this.sbf = sbf;
  }

  @Bean
  public Job s3SyncJob(Step s3SyncStep) {
    return jbf.get(BatchJobType.SYNC_BILLING_REPORT_S3.name())
        .incrementer(new RunIdIncrementer())
        .start(s3SyncStep)
        .build();
  }

  @Bean
  public Step s3SyncStep(ItemReader<?> s3SyncReader, ItemWriter<? super Object> s3SyncWriter) {
    return sbf.get("s3SyncStep").chunk(BATCH_SIZE).reader(s3SyncReader).writer(s3SyncWriter).build();
  }

  @Bean
  @StepScope
  public ItemReader<S3SyncRecord> s3SyncReader() {
    return null;
  }

  @Bean
  @StepScope
  public ItemWriter<S3SyncRecord> s3SyncWriter() {
    return null;
  }

  private static class S3SyncRecord {}
}
