package io.harness.batch.processing.metrics;

import java.time.Instant;

public interface CeCloudMetricsService {
  double getTotalCloudCost(String accountId, String cloudProviderType, Instant start, Instant end);
}
