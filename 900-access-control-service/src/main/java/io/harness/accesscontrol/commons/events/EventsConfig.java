package io.harness.accesscontrol.commons.events;

import io.harness.redis.RedisConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EventsConfig {
  @JsonProperty("redis") RedisConfig redisConfig;
  boolean enabled;
}
