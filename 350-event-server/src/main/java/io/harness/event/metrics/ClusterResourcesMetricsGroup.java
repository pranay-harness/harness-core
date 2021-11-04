package io.harness.event.metrics;

import io.harness.metrics.AutoMetricContext;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class ClusterResourcesMetricsGroup extends AutoMetricContext {
  public ClusterResourcesMetricsGroup(String accountId, String clusterId) {
    put("accountId", accountId);
    put("clusterId", clusterId);
  }
}