package io.harness.eventsframework.impl.redis.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.impl.redis.context.RedisEventContext;
import io.harness.eventsframework.impl.redis.dto.RedisEventDTO;
import io.harness.metrics.service.api.MetricService;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class RedisEventStatsPublisher {
  private final MetricService metricService;

  public void sendMetricWithEventContext(RedisEventDTO redisEventDTO, String metricName) {
    try (RedisEventContext context = new RedisEventContext(redisEventDTO)) {
      metricService.incCounter(metricName);
    }
  }
}
