package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.core.services.entities.TimeSeriesThreshold;

import java.util.List;

public interface MetricPackService {
  List<MetricPack> getMetricPacks(String accountId, String projectIdentifier, DataSourceType dataSourceType);

  boolean saveMetricPacks(
      String accountId, String projectIdentifier, DataSourceType dataSourceType, List<MetricPack> metricPacks);

  List<TimeSeriesThreshold> getMetricPackThresholds(
      String accountId, String projectIdentifier, String metricPackIdentifier, DataSourceType dataSourceType);

  List<String> saveMetricPackThreshold(String accountId, String projectIdentifier, DataSourceType dataSourceType,
      List<TimeSeriesThreshold> timeSeriesThreshold);

  boolean deleteMetricPackThresholds(String accountId, String projectIdentifier, String thresholdId);

  void populatePaths(String accountId, String projectIdentifier, DataSourceType dataSourceType, MetricPack metricPack);
}
