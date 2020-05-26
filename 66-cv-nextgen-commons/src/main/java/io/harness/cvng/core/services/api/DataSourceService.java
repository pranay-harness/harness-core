package io.harness.cvng.core.services.api;

import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.models.DataSourceType;

import java.util.Collection;
import java.util.List;

public interface DataSourceService {
  Collection<MetricPack> getMetricPacks(String accountId, String projectId, DataSourceType dataSourceType);

  boolean saveMetricPacks(
      String accountId, String projectId, DataSourceType dataSourceType, List<MetricPack> metricPacks);
}
