package io.harness.eventsframework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.redis.RedisConfig;
import io.harness.secret.ConfigSecret;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventsFrameworkConfiguration {
  @JsonProperty("redis") @ConfigSecret RedisConfig redisConfig;
}
