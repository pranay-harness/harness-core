package io.harness.aggregator;

import java.util.Optional;

public interface AggregatorMetricsService {
  Optional<SnapshotMetrics> getSnapshotMetrics();

  Optional<StreamingMetrics> getStreamingMetrics();
}
