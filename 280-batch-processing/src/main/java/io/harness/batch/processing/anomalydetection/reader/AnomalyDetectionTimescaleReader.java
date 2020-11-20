package io.harness.batch.processing.anomalydetection.reader;

import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.service.impl.AnomalyDetectionTimescaleDataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public abstract class AnomalyDetectionTimescaleReader
    implements ItemReader<AnomalyDetectionTimeSeries>, StepExecutionListener {
  @Autowired AnomalyDetectionTimescaleDataServiceImpl dataService;
  List<AnomalyDetectionTimeSeries> listAnomalyDetectionTimeSeries;
  JobParameters parameters;
  String accountId;
  int timeSeriesIndex;

  @Override
  public AnomalyDetectionTimeSeries read() {
    AnomalyDetectionTimeSeries anomalyDetectionTimeSeries = null;
    if (timeSeriesIndex < listAnomalyDetectionTimeSeries.size()) {
      anomalyDetectionTimeSeries = listAnomalyDetectionTimeSeries.get(timeSeriesIndex);
      timeSeriesIndex++;
    }
    return anomalyDetectionTimeSeries;
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    log.debug("Time Series Reader ended.");
    return ExitStatus.COMPLETED;
  }
}
