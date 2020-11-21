package io.harness.batch.processing.anomalydetection.reader;

import io.harness.batch.processing.anomalydetection.AnomalyDetectionConstants;
import io.harness.batch.processing.anomalydetection.TimeSeriesSpec;
import io.harness.batch.processing.anomalydetection.types.EntityType;
import io.harness.batch.processing.anomalydetection.types.TimeGranularity;
import io.harness.batch.processing.ccm.CCMJobConstants;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;

@Slf4j
public class AnomalyDetectionNamespaceTimescaleReader extends AnomalyDetectionTimescaleReader {
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
            .entityType(EntityType.NAMESPACE)
            .entityIdentifier("NAMESPACE")
            .build();
    log.info("Anomaly Detection batch job of {} type , {} time granularity {}, for accountId:{} , endtime:{}",
        EntityType.CLUSTER.toString(), TimeGranularity.DAILY.toString(), accountId, endTime.toString());
    listAnomalyDetectionTimeSeries = dataService.readData(timeSeriesSpec);
    log.info("successfully read {} no of {}", listAnomalyDetectionTimeSeries.size(),
        timeSeriesSpec.getEntityType().toString());
    timeSeriesIndex = 0;
  }
}
