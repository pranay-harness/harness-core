package io.harness.service.stats.usagemetrics.eventpublisher;

import io.harness.dto.Instance;

import java.util.List;

public interface UsageMetricsEventPublisher {
  void publishInstanceStatsTimeSeries(String accountId, long timestamp, List<Instance> instances);
}
