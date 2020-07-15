package io.harness.cvng.core.services.api;

import io.harness.cvng.analysis.beans.TimeSeriesTestDataDTO;
import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;

import java.time.Instant;
import java.util.List;

public interface TimeSeriesService {
  boolean save(List<TimeSeriesDataCollectionRecord> dataRecords);

  List<TimeSeriesMetricDefinition> getTimeSeriesMetricDefinitions(String cvConfigId);
  TimeSeriesTestDataDTO getTxnMetricDataForRange(
      String cvConfigId, Instant startTime, Instant endTime, String metricName, String txnName);
  TimeSeriesTestDataDTO getMetricGroupDataForRange(
      String cvConfigId, Instant startTime, Instant endTime, String metricName, List<String> groupNames);
}
