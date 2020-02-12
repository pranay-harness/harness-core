package io.harness.lock.redis;

import com.google.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author rktummala on 05/08/19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class RedisConfig {
  @JsonProperty(defaultValue = "false") private boolean enabled;
  private boolean sentinel;
  private String masterName;
  private String redisUrl;
  private List<String> sentinelUrls;
}
