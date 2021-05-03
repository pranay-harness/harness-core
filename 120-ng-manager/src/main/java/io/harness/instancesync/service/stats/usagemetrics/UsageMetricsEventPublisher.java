package io.harness.instancesync.service.stats.usagemetrics;

import io.harness.instancesync.dto.Instance;

import java.util.List;

public interface UsageMetricsEventPublisher {
  void publishInstanceStatsTimeSeries(String accountId, long timestamp, List<Instance> instances);
}
