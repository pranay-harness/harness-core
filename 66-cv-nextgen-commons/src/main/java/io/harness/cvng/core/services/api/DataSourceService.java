package io.harness.cvng.core.services.api;

import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.models.DataSourceType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DataSourceService {
  Map<String, MetricPack> getMetricPackMap(String accountId, String projectId, DataSourceType dataSourceType);

  Collection<MetricPack> getMetricPacks(
      String accountId, String projectId, DataSourceType dataSourceType, boolean excludeDetails);

  boolean saveMetricPacks(
      String accountId, String projectId, DataSourceType dataSourceType, List<MetricPack> metricPacks);
}
