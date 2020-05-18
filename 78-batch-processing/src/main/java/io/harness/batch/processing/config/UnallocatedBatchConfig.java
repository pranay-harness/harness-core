package io.harness.batch.processing.config;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.UnallocatedCostData;
import io.harness.batch.processing.reader.UnallocatedBillingDataReader;
import io.harness.batch.processing.writer.UnallocatedBillingDataWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class UnallocatedBatchConfig {
  private static final int BATCH_SIZE = 100;

  @Bean
  public ItemReader<List<UnallocatedCostData>> unallocatedAggregatedCostReader() {
    return new UnallocatedBillingDataReader();
  }

  @Bean
  public ItemWriter<List<UnallocatedCostData>> unallocatedAggregatedCostWriter() {
    return new UnallocatedBillingDataWriter();
  }

  @Bean
  @Qualifier(value = "unallocatedCostJob")
  public Job unallocatedCostJob(JobBuilderFactory jobBuilderFactory, Step unallocatedCostCalculationStep) {
    return jobBuilderFactory.get(BatchJobType.UNALLOCATED_BILLING.name())
        .incrementer(new RunIdIncrementer())
        .start(unallocatedCostCalculationStep)
        .build();
  }

  @Bean
  @Qualifier(value = "unallocatedHourlyCostJob")
  public Job unallocatedHourlyCostJob(JobBuilderFactory jobBuilderFactory, Step unallocatedCostCalculationStep) {
    return jobBuilderFactory.get(BatchJobType.UNALLOCATED_BILLING_HOURLY.name())
        .incrementer(new RunIdIncrementer())
        .start(unallocatedCostCalculationStep)
        .build();
  }

  @Bean
  public Step unallocatedCostCalculationStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("unallocatedCostCalculationStep")
        .<List<UnallocatedCostData>, List<UnallocatedCostData>>chunk(BATCH_SIZE)
        .reader(unallocatedAggregatedCostReader())
        .writer(unallocatedAggregatedCostWriter())
        .build();
  }
}
