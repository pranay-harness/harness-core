package io.harness.eventsframework.monitor.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.monitor.context.RedisStreamAccountContext;
import io.harness.eventsframework.monitor.dto.RedisStreamAccountDTO;
import io.harness.metrics.service.api.MetricService;
import io.harness.metrics.service.api.MetricsPublisher;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import service.RedisStreamsMetricsAggregator;

@OwnedBy(HarnessTeam.PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class RedisStreamAccountStatsPublisher implements MetricsPublisher {
  private final MetricService metricService;
  private final RedisStreamsMetricsAggregator redisStreamsMetricsAggregator;

  private void sendMetricWithStreamAccountContext(
      RedisStreamAccountDTO redisStreamAccountDTO, String metricName, double value) {
    try (RedisStreamAccountContext context = new RedisStreamAccountContext(redisStreamAccountDTO)) {
      metricService.recordMetric(metricName, value);
    }
  }

  @Override
  public void recordMetrics() {}
}
