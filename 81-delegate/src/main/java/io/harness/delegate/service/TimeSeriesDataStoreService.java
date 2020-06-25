package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.TreeBasedTable;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;
import io.harness.datacollection.entity.TimeSeriesRecord;
import io.harness.rest.RestResponse;
import io.harness.verificationclient.CVNextGenServiceClient;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
      String accountId, String cvConfigId, List<TimeSeriesRecord> timeSeriesRecords) {
    List<TimeSeriesDataCollectionRecord> dataCollectionRecords = new ArrayList<>();
    TreeBasedTable<Long, String, TimeSeriesDataCollectionRecord.TimeSeriesDataRecordMetricValue> metricValues =
        TreeBasedTable.create();
    timeSeriesRecords.forEach(timeSeriesRecord -> {
      final long timeStamp = timeSeriesRecord.getTimestamp();
      final String metricName = timeSeriesRecord.getMetricName();

      if (!metricValues.contains(timeStamp, metricName)) {
        metricValues.put(timeStamp, metricName,
            TimeSeriesDataCollectionRecord.TimeSeriesDataRecordMetricValue.builder()
                .metricName(metricName)
                .timeSeriesValues(new HashSet<>())
                .build());
      }

      metricValues.get(timeStamp, metricName)
          .getTimeSeriesValues()
          .add(TimeSeriesDataCollectionRecord.TimeSeriesDataRecordGroupValue.builder()
                   .groupName(timeSeriesRecord.getTxnName())
                   .value(timeSeriesRecord.getMetricValue())
                   .build());
    });
    metricValues.rowMap().forEach((timeStamp, groupValueMap) -> {
      final TimeSeriesDataCollectionRecord dataCollectionRecord = TimeSeriesDataCollectionRecord.builder()
                                                                      .accountId(accountId)
                                                                      .cvConfigId(cvConfigId)
                                                                      .timeStamp(timeStamp)
                                                                      .metricValues(new HashSet<>())
                                                                      .build();

      groupValueMap.forEach((metricName, dataRecordMetricValue) -> {
        dataCollectionRecord.getMetricValues().add(dataRecordMetricValue);
      });
      dataCollectionRecords.add(dataCollectionRecord);
    });
    return dataCollectionRecords;
  }

  public boolean saveTimeSeriesDataRecords(
      String accountId, String cvConfigId, List<TimeSeriesRecord> timeSeriesRecords) {
    if (timeSeriesRecords.isEmpty()) {
      return true;
    }
    List<TimeSeriesDataCollectionRecord> dataCollectionRecords =
        convertToCollectionRecords(accountId, cvConfigId, timeSeriesRecords);
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
      logger.error("error saving new apm metrics", e);
      return false;
    }
  }
}
