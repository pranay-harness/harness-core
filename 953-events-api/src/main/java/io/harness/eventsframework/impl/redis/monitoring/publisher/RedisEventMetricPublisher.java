package io.harness.eventsframework.impl.redis.monitoring.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.impl.redis.monitoring.context.RedisEventMetricContext;
import io.harness.eventsframework.impl.redis.monitoring.dto.RedisEventMetricDTO;
import io.harness.metrics.service.api.MetricService;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class RedisEventMetricPublisher {
  private final MetricService metricService;

  public void sendMetricWithEventContext(RedisEventMetricDTO redisEventMetricDTO, String metricName) {
    try (RedisEventMetricContext context = new RedisEventMetricContext(redisEventMetricDTO)) {
      metricService.incCounter(metricName);
    }
  }
}
