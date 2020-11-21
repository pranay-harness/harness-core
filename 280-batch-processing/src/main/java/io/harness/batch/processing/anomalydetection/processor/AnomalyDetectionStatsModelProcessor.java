package io.harness.batch.processing.anomalydetection.processor;

import io.harness.batch.processing.anomalydetection.Anomaly;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.models.StatsModel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public class AnomalyDetectionStatsModelProcessor
    implements ItemProcessor<AnomalyDetectionTimeSeries, Anomaly>, StepExecutionListener {
  @Override
  public void beforeStep(StepExecution stepExecution) {
    log.info("TimeSeries Processor initialized.");
  }

  @Override
  public Anomaly process(AnomalyDetectionTimeSeries data) {
    log.info("processing {} {}", data.getEntityType().toString(), data.getEntityId());
    StatsModel model = StatsModel.builder().build();
    return model.detectAnomaly(data).get(0);
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    log.info("TimeSeries Processor ended.");
    return ExitStatus.COMPLETED;
  }
}
