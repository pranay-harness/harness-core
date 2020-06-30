package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;

import java.util.List;

public interface TimeSeriesService {
  boolean save(List<TimeSeriesDataCollectionRecord> dataRecords);

  List<TimeSeriesMetricDefinition> getTimeSeriesMetricDefinitions(String cvConfigId);
}
