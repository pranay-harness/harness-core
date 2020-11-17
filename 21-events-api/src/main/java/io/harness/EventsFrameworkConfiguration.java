package io.harness;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.redis.RedisConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventsFrameworkConfiguration {
  @JsonProperty("redis") RedisConfig redisConfig;
}
