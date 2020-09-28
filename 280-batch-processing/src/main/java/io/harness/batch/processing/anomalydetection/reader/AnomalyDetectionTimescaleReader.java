package io.harness.batch.processing.anomalydetection.reader;

import io.harness.batch.processing.anomalydetection.AnomalyDetectionConstants;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.TimeSeriesSpec;
import io.harness.batch.processing.anomalydetection.service.impl.AnomalyDetectionTimescaleDataServiceImpl;
import io.harness.batch.processing.anomalydetection.types.EntityType;
import io.harness.batch.processing.anomalydetection.types.TimeGranularity;
import io.harness.batch.processing.ccm.CCMJobConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
public class AnomalyDetectionTimescaleReader implements ItemReader<AnomalyDetectionTimeSeries>, StepExecutionListener {
  @Autowired private AnomalyDetectionTimescaleDataServiceImpl dataService;
  private List<AnomalyDetectionTimeSeries> listClusterAnomalyDetectionTimeSeries;
  private JobParameters parameters;
  private String accountId;
  private int timeSeriesIndex;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
    accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Instant endTime = CCMJobConstants.getFieldValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE);
    TimeSeriesSpec timeSeriesSpec =
        TimeSeriesSpec.builder()
            .accountId(accountId)
            .trainStart(endTime.minus(AnomalyDetectionConstants.DAYS_TO_CONSIDER, ChronoUnit.DAYS))
            .trainEnd(endTime.minus(1, ChronoUnit.DAYS))
            .testStart(endTime.minus(1, ChronoUnit.DAYS))
            .testEnd(endTime)
            .timeGranularity(TimeGranularity.DAILY)
            .entityType(EntityType.CLUSTER)
            .build();
    logger.info("Anomaly Detection batch job of {} type , {} time granularity {}, for accountId:{} , endtime:{}",
        EntityType.CLUSTER.toString(), TimeGranularity.DAILY.toString(), accountId, endTime.toString());
    listClusterAnomalyDetectionTimeSeries = dataService.readClusterLevelData(timeSeriesSpec);
    logger.info("successfully read {} no of clusters", listClusterAnomalyDetectionTimeSeries.size());
    timeSeriesIndex = 0;
  }

  @Override
  public AnomalyDetectionTimeSeries read() {
    AnomalyDetectionTimeSeries anomalyDetectionTimeSeries = null;
    if (timeSeriesIndex < listClusterAnomalyDetectionTimeSeries.size()) {
      anomalyDetectionTimeSeries = listClusterAnomalyDetectionTimeSeries.get(timeSeriesIndex);
      timeSeriesIndex++;
    }
    return anomalyDetectionTimeSeries;
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    logger.debug("Time Series Reader ended.");
    return ExitStatus.COMPLETED;
  }
}