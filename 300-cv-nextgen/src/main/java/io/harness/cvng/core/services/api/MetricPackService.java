package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.TimeSeriesThreshold;

import java.util.List;

public interface MetricPackService {
  List<MetricPackDTO> getMetricPacks(
      DataSourceType dataSourceType, String accountId, String orgIdentifier, String projectIdentifier);

  List<MetricPack> getMetricPacks(
      String accountId, String orgIdentifier, String projectIdentifier, DataSourceType dataSourceType);

  boolean saveMetricPacks(String accountId, String orgIdentifier, String projectIdentifier,
      DataSourceType dataSourceType, List<MetricPack> metricPacks);

  List<TimeSeriesThreshold> getMetricPackThresholds(String accountId, String orgIdentifier, String projectIdentifier,
      String metricPackIdentifier, DataSourceType dataSourceType);

  List<String> saveMetricPackThreshold(String accountId, String orgIdentifier, String projectIdentifier,
      DataSourceType dataSourceType, List<TimeSeriesThreshold> timeSeriesThreshold);

  boolean deleteMetricPackThresholds(
      String accountId, String orgIdentifier, String projectIdentifier, String thresholdId);

  void populatePaths(String accountId, String orgIdentifier, String projectIdentifier, DataSourceType dataSourceType,
      MetricPack metricPack);

  void populateDataCollectionDsl(DataSourceType dataSourceType, MetricPack metricPack);
}
