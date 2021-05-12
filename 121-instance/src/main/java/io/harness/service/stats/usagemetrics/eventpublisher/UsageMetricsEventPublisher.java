package io.harness.service.stats.usagemetrics.eventpublisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.instance.Instance;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public interface UsageMetricsEventPublisher {
  void publishInstanceStatsTimeSeries(String accountId, long timestamp, List<Instance> instances);
}
