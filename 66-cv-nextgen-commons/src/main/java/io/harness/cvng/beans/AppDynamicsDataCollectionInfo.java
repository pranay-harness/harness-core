package io.harness.cvng.beans;

import io.harness.cvng.core.services.entities.MetricPack;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
public class AppDynamicsDataCollectionInfo extends TimeSeriesDataCollectionInfo {
  private long tierId;
  private long applicationId;
  private MetricPack metricPack;

  @Override
  public Map<String, Object> getDslEnvVariables() {
    Map<String, Object> dslEnvVariables = new HashMap<>();
    dslEnvVariables.put("appId", getApplicationId());
    dslEnvVariables.put("tierId", getTierId());
    final List<String> metricPaths = getMetricPack()
                                         .getMetrics()
                                         .stream()
                                         .filter(metricDefinition -> metricDefinition.isIncluded())
                                         .map(metricDefinition -> metricDefinition.getPath())
                                         .collect(Collectors.toList());
    dslEnvVariables.put("metricsToCollect", metricPaths);
    return dslEnvVariables;
  }
}
