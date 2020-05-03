package io.harness.redis;

import com.google.inject.Singleton;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class RedisConfig {
  private boolean sentinel;
  private String masterName;
  private String redisUrl;
  private List<String> sentinelUrls;
  private String envNamespace;
}
