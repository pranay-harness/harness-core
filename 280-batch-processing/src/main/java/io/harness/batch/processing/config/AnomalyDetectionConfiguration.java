package io.harness.batch.processing.config;

import io.harness.batch.processing.anomalydetection.Anomaly;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionConstants;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.processor.AnomalyDetectionStatsModelProcessor;
import io.harness.batch.processing.anomalydetection.reader.AnomalyDetectionClusterTimescaleReader;
import io.harness.batch.processing.anomalydetection.reader.AnomalyDetectionNamespaceTimescaleReader;
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
  public Job anomalyDetectionDailyJob(JobBuilderFactory jobBuilderFactory,
      Step statisticalModelClusterAnomalyDetectionStep, Step statisticalModelNamespaceAnomalyDetectionStep) {
    return jobBuilderFactory.get(BatchJobType.ANOMALY_DETECTION.name())
        .incrementer(new RunIdIncrementer())
        .start(statisticalModelNamespaceAnomalyDetectionStep)
        .next(statisticalModelClusterAnomalyDetectionStep)
        .build();
  }

  @Bean
  protected Step statisticalModelClusterAnomalyDetectionStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("statisticalModelDailyAnomalyDetectionStep")
        .<AnomalyDetectionTimeSeries, Anomaly>chunk(AnomalyDetectionConstants.BATCH_SIZE)
        .reader(anomalyDetectionClusterTimescaleItemReader())
        .processor(anomalyDetectionStatModelProcessor())
        .writer(anomalyDetectionTimescaleWriter())
        .build();
  }

  @Bean
  protected Step statisticalModelNamespaceAnomalyDetectionStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("statisticalModelDailyAnomalyDetectionStep")
        .<AnomalyDetectionTimeSeries, Anomaly>chunk(AnomalyDetectionConstants.BATCH_SIZE)
        .reader(anomalyDetectionNamespaceTimescaleItemReader())
        .processor(anomalyDetectionStatModelProcessor())
        .writer(anomalyDetectionTimescaleWriter())
        .build();
  }

  @Bean
  public ItemReader<AnomalyDetectionTimeSeries> anomalyDetectionClusterTimescaleItemReader() {
    return new AnomalyDetectionClusterTimescaleReader();
  }

  @Bean
  public ItemReader<AnomalyDetectionTimeSeries> anomalyDetectionNamespaceTimescaleItemReader() {
    return new AnomalyDetectionNamespaceTimescaleReader();
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
