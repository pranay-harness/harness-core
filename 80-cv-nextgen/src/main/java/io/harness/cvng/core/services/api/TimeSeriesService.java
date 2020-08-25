package io.harness.cvng.core.services.api;

import io.harness.cvng.analysis.beans.TimeSeriesTestDataDTO;
import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.core.beans.TimeSeriesRecordDTO;

import java.time.Instant;
import java.util.List;

public interface TimeSeriesService {
  boolean save(List<TimeSeriesDataCollectionRecord> dataRecords);

  List<TimeSeriesMetricDefinition> getTimeSeriesMetricDefinitions(String cvConfigId);
  TimeSeriesTestDataDTO getTxnMetricDataForRange(
      String cvConfigId, Instant startTime, Instant endTime, String metricName, String txnName);

  /**
   * startTime is inclusive and endTime is exclusive.
   * @param verificationTaskId
   * @param startTime inclusive
   * @param endTime exclusive
   * @return
   */
  List<TimeSeriesRecordDTO> getTimeSeriesRecordDTOs(String verificationTaskId, Instant startTime, Instant endTime);
  TimeSeriesTestDataDTO getMetricGroupDataForRange(
      String cvConfigId, Instant startTime, Instant endTime, String metricName, List<String> groupNames);
}
