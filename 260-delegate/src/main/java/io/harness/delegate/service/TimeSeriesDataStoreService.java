package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import static java.util.stream.Collectors.groupingBy;

import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;
import io.harness.cvng.beans.TimeSeriesDataCollectionRecord.TimeSeriesDataRecordGroupValue;
import io.harness.cvng.beans.TimeSeriesDataCollectionRecord.TimeSeriesDataRecordMetricValue;
import io.harness.datacollection.entity.TimeSeriesRecord;
import io.harness.rest.RestResponse;
import io.harness.verificationclient.CVNextGenServiceClient;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by raghu on 5/19/17.
 */
@Singleton
@Slf4j
public class TimeSeriesDataStoreService {
  @Inject private CVNextGenServiceClient cvNextGenServiceClient;
  @Inject private TimeLimiter timeLimiter;

  @VisibleForTesting
  List<TimeSeriesDataCollectionRecord> convertToCollectionRecords(
      String accountId, String verificationTaskId, List<TimeSeriesRecord> timeSeriesRecords) {
    Map<TimeSeriesRecordBucketKey, List<TimeSeriesRecord>> groupByHostAndTimestamp =
        timeSeriesRecords.stream().collect(groupingBy(timeSeriesRecord
            -> TimeSeriesRecordBucketKey.builder()
                   .host(timeSeriesRecord.getHostname())
                   .timestamp(timeSeriesRecord.getTimestamp())
                   .build()));

    List<TimeSeriesDataCollectionRecord> dataCollectionRecords = new ArrayList<>();
    groupByHostAndTimestamp.forEach((timeSeriesRecordBucketKey, timeSeriesRecordsGroup) -> {
      TimeSeriesDataCollectionRecord timeSeriesDataCollectionRecord =
          TimeSeriesDataCollectionRecord.builder()
              .accountId(accountId)
              .verificationTaskId(verificationTaskId)
              .timeStamp(timeSeriesRecordBucketKey.getTimestamp())
              .host(timeSeriesRecordBucketKey.getHost())
              .metricValues(new HashSet<>())
              .build();
      timeSeriesRecordsGroup.stream()
          .collect(groupingBy(TimeSeriesRecord::getMetricName))
          .forEach((metricName, timeSeriesRecordsGroupByMetricName) -> {
            TimeSeriesDataRecordMetricValue timeSeriesDataRecordMetricValue = TimeSeriesDataRecordMetricValue.builder()
                                                                                  .metricName(metricName)
                                                                                  .timeSeriesValues(new HashSet<>())
                                                                                  .build();
            timeSeriesRecordsGroupByMetricName.forEach(timeSeriesRecord
                -> timeSeriesDataRecordMetricValue.getTimeSeriesValues().add(
                    TimeSeriesDataRecordGroupValue.builder()
                        .groupName(timeSeriesRecord.getTxnName())
                        .value(timeSeriesRecord.getMetricValue())
                        .build()));
            timeSeriesDataCollectionRecord.getMetricValues().add(timeSeriesDataRecordMetricValue);
          });
      dataCollectionRecords.add(timeSeriesDataCollectionRecord);
    });
    return dataCollectionRecords;
  }
  @Value
  @Builder
  private static class TimeSeriesRecordBucketKey {
    String host;
    long timestamp;
  }

  public boolean saveTimeSeriesDataRecords(
      String accountId, String verificationTaskId, List<TimeSeriesRecord> timeSeriesRecords) {
    if (timeSeriesRecords.isEmpty()) {
      log.info(
          "TimeseriesRecords is empty. So we will not be saving anything from the delegate for {}", verificationTaskId);
      return true;
    }
    List<TimeSeriesDataCollectionRecord> dataCollectionRecords =
        convertToCollectionRecords(accountId, verificationTaskId, timeSeriesRecords);
    try {
      RestResponse<Boolean> restResponse = timeLimiter.callWithTimeout(
          ()
              -> execute(cvNextGenServiceClient.saveTimeSeriesMetrics(accountId, dataCollectionRecords)),
          30, TimeUnit.SECONDS, true);
      if (restResponse == null) {
        return false;
      }

      return restResponse.getResource();
    } catch (Exception e) {
      log.error("error saving new apm metrics", e);
      return false;
    }
  }
}
