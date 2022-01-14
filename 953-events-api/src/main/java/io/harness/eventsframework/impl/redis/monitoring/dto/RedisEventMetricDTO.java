package io.harness.eventsframework.impl.redis.monitoring.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class RedisEventMetricDTO {
  private String accountId;
}
