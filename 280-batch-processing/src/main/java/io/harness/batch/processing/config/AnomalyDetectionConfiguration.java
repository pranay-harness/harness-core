package io.harness.batch.processing.config;

import io.harness.batch.processing.anomalydetection.AnomalyDetectionConstants;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.processor.AnomalyDetectionStatsModelProcessor;
import io.harness.batch.processing.anomalydetection.reader.cloud.AnomalyDetectionAwsAccountReader;
import io.harness.batch.processing.anomalydetection.reader.cloud.AnomalyDetectionAwsServiceReader;
import io.harness.batch.processing.anomalydetection.reader.cloud.AnomalyDetectionGcpProductReader;
import io.harness.batch.processing.anomalydetection.reader.cloud.AnomalyDetectionGcpProjectReader;
import io.harness.batch.processing.anomalydetection.reader.cloud.AnomalyDetectionGcpSkuReader;
import io.harness.batch.processing.anomalydetection.reader.k8s.AnomalyDetectionClusterTimescaleReader;
import io.harness.batch.processing.anomalydetection.reader.k8s.AnomalyDetectionNamespaceTimescaleReader;
import io.harness.batch.processing.anomalydetection.types.Anomaly;
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
  public Job anomalyDetectionDailyJob(JobBuilderFactory jobBuilderFactory, Step statisticalModelClusterStep,
      Step statisticalModelNamespaceStep, Step statisticalModelGcpProjectStep, Step statisticalModelGcpSkuStep,
      Step statisticalModelGcpProductStep, Step statisticalModelAwsAccountStep, Step statisticalModelAwsServiceStep) {
    return jobBuilderFactory.get(BatchJobType.ANOMALY_DETECTION.name())
        .incrementer(new RunIdIncrementer())
        .start(statisticalModelClusterStep)
        .next(statisticalModelNamespaceStep)
        .next(statisticalModelGcpProjectStep)
        .next(statisticalModelGcpProductStep)
        .next(statisticalModelGcpSkuStep)
        .next(statisticalModelAwsAccountStep)
        .next(statisticalModelAwsServiceStep)
        .build();
  }

  @Bean
  protected Step statisticalModelClusterStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("statisticalModelClusterDailyAnomalyDetectionStep")
        .<AnomalyDetectionTimeSeries, Anomaly>chunk(AnomalyDetectionConstants.BATCH_SIZE)
        .reader(clusterItemReader())
        .processor(statModelProcessor())
        .writer(timescaleWriter())
        .build();
  }

  @Bean
  protected Step statisticalModelNamespaceStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("statisticalModelNamespaceDailyAnomalyDetectionStep")
        .<AnomalyDetectionTimeSeries, Anomaly>chunk(AnomalyDetectionConstants.BATCH_SIZE)
        .reader(namespaceItemReader())
        .processor(statModelProcessor())
        .writer(timescaleWriter())
        .build();
  }

  @Bean
  protected Step statisticalModelGcpProjectStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("statisticalModelGcpProjectDailyAnomalyDetectionStep")
        .<AnomalyDetectionTimeSeries, Anomaly>chunk(AnomalyDetectionConstants.BATCH_SIZE)
        .reader(gcpProjectItemReader())
        .processor(statModelProcessor())
        .writer(timescaleWriter())
        .build();
  }

  @Bean
  protected Step statisticalModelGcpProductStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("statisticalModelGcpProductDailyAnomalyDetectionStep")
        .<AnomalyDetectionTimeSeries, Anomaly>chunk(AnomalyDetectionConstants.BATCH_SIZE)
        .reader(gcpProductItemReader())
        .processor(statModelProcessor())
        .writer(timescaleWriter())
        .build();
  }

  @Bean
  protected Step statisticalModelGcpSkuStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("statisticalModelGcpSkuDailyAnomalyDetectionStep")
        .<AnomalyDetectionTimeSeries, Anomaly>chunk(AnomalyDetectionConstants.BATCH_SIZE)
        .reader(gcpSkuItemReader())
        .processor(statModelProcessor())
        .writer(timescaleWriter())
        .build();
  }

  @Bean
  protected Step statisticalModelAwsAccountStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("statisticalModelAwsAccountDailyAnomalyDetectionStep")
        .<AnomalyDetectionTimeSeries, Anomaly>chunk(AnomalyDetectionConstants.BATCH_SIZE)
        .reader(awsAccountItemReader())
        .processor(statModelProcessor())
        .writer(timescaleWriter())
        .build();
  }

  @Bean
  protected Step statisticalModelAwsServiceStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("statisticalModelAwsServiceDailyAnomalyDetectionStep")
        .<AnomalyDetectionTimeSeries, Anomaly>chunk(AnomalyDetectionConstants.BATCH_SIZE)
        .reader(awsServiceItemReader())
        .processor(statModelProcessor())
        .writer(timescaleWriter())
        .build();
  }

  // ---------------- Item Reader ----------------------
  @Bean
  public ItemReader<AnomalyDetectionTimeSeries> clusterItemReader() {
    return new AnomalyDetectionClusterTimescaleReader();
  }

  @Bean
  public ItemReader<AnomalyDetectionTimeSeries> namespaceItemReader() {
    return new AnomalyDetectionNamespaceTimescaleReader();
  }

  @Bean
  public ItemReader<AnomalyDetectionTimeSeries> gcpProjectItemReader() {
    return new AnomalyDetectionGcpProjectReader();
  }

  @Bean
  public ItemReader<AnomalyDetectionTimeSeries> gcpProductItemReader() {
    return new AnomalyDetectionGcpProductReader();
  }

  @Bean
  public ItemReader<AnomalyDetectionTimeSeries> gcpSkuItemReader() {
    return new AnomalyDetectionGcpSkuReader();
  }

  @Bean
  public ItemReader<AnomalyDetectionTimeSeries> awsAccountItemReader() {
    return new AnomalyDetectionAwsAccountReader();
  }

  @Bean
  public ItemReader<AnomalyDetectionTimeSeries> awsServiceItemReader() {
    return new AnomalyDetectionAwsServiceReader();
  }

  // ---------------- Item Processor ----------------------
  @Bean
  public ItemProcessor<AnomalyDetectionTimeSeries, Anomaly> statModelProcessor() {
    return new AnomalyDetectionStatsModelProcessor();
  }

  // ---------------- Item Writer ----------------------
  @Bean
  public ItemWriter<Anomaly> timescaleWriter() {
    return new AnomalyDetectionTimeScaleWriter();
  }
}
