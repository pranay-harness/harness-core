package io.harness.accesscontrol.commons.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.redis.RedisConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class EventsConfig {
  @JsonProperty("redis") RedisConfig redisConfig;
  boolean enabled;
}
