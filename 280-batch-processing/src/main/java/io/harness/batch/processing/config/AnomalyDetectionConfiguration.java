package io.harness.batch.processing.config;

import io.harness.batch.processing.anomalydetection.Anomaly;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionConstants;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.processor.AnomalyDetectionStatsModelProcessor;
import io.harness.batch.processing.anomalydetection.reader.AnomalyDetectionTimescaleReader;
import io.harness.batch.processing.anomalydetection.writer.AnomalyDetectionTimeScaleWriter;
import io.harness.batch.processing.ccm.BatchJobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AnomalyDetectionConfiguration {
  @Bean
  @Qualifier(value = "anomalyDetectionDailyJob")
  public Job anomalyDetectionDailyJob(
      JobBuilderFactory jobBuilderFactory, Step statisticalModelDailyAnomalyDetectionStep) {
    return jobBuilderFactory.get(BatchJobType.ANOMALY_DETECTION.name())
        .incrementer(new RunIdIncrementer())
        .start(statisticalModelDailyAnomalyDetectionStep)
        .build();
  }

  @Bean
  protected Step statisticalModelDailyAnomalyDetectionStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("statisticalModelDailyAnomalyDetectionStep")
        .<AnomalyDetectionTimeSeries, Anomaly>chunk(AnomalyDetectionConstants.BATCH_SIZE)
        .reader(anomalyDetectionTimescaleItemReader())
        .processor(anomalyDetectionStatModelProcessor())
        .writer(anomalyDetectionTimescaleWriter())
        .build();
  }

  @Bean
  public ItemReader<AnomalyDetectionTimeSeries> anomalyDetectionTimescaleItemReader() {
    return new AnomalyDetectionTimescaleReader();
  }

  @Bean
  public ItemProcessor<AnomalyDetectionTimeSeries, Anomaly> anomalyDetectionStatModelProcessor() {
    return new AnomalyDetectionStatsModelProcessor();
  }

  @Bean
  public ItemWriter<Anomaly> anomalyDetectionTimescaleWriter() {
    return new AnomalyDetectionTimeScaleWriter();
  }
}
