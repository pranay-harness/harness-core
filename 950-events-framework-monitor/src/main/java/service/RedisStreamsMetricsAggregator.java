package service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.monitor.dto.AggregateRedisStreamMetricsDTO;

@OwnedBy(HarnessTeam.PL)
public interface RedisStreamsMetricsAggregator {
  AggregateRedisStreamMetricsDTO getStreamStats();
}
