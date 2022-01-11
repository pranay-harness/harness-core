/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
