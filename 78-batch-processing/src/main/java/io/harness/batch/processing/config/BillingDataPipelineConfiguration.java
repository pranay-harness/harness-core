package io.harness.batch.processing.config;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.reader.SettingAttributeReader;
import io.harness.batch.processing.writer.BillingDataPipelineWriter;
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
public class BillingDataPipelineConfiguration {
  private static final int BATCH_SIZE = 10;

  @Bean
  public ItemWriter<SettingAttribute> awsBillingDataPipelineWriter() {
    return new BillingDataPipelineWriter();
  }

  @Bean
  @Autowired
  @Qualifier(value = "awsBillingDataPipelineJob")
  public Job awsBillingDataPipelineJob(JobBuilderFactory jobBuilderFactory, Step awsBillingDataPipelineStep) {
    return jobBuilderFactory.get(BatchJobType.BILLING_DATA_PIPELINE.name())
        .incrementer(new RunIdIncrementer())
        .start(awsBillingDataPipelineStep)
        .build();
  }

  @Bean
  public Step awsBillingDataPipelineStep(
      StepBuilderFactory stepBuilderFactory, SettingAttributeReader settingAttributeReader) {
    return stepBuilderFactory.get("awsBillingDataPipelineStep")
        .<SettingAttribute, SettingAttribute>chunk(BATCH_SIZE)
        .reader(settingAttributeReader)
        .writer(awsBillingDataPipelineWriter())
        .build();
  }
}
