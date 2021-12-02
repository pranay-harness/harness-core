package io.harness.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.impl.redis.RedisUtils;
import io.harness.redis.RedisConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.redisson.api.RedissonClient;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsRedissonClientFactory {
  private static final Map<RedisConfig, RedissonClient> redisConfigRedissonClientMap = new ConcurrentHashMap<>();

  public static RedissonClient getRedisClient(RedisConfig redisConfig) {
    if (!redisConfigRedissonClientMap.containsKey(redisConfig)) {
      redisConfigRedissonClientMap.put(redisConfig, RedisUtils.getClient(redisConfig));
    }
    return redisConfigRedissonClientMap.get(redisConfig);
  }
}
