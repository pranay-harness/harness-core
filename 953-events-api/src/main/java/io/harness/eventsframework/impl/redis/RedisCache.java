package io.harness.eventsframework.impl.redis;

import io.harness.redis.RedisConfig;

import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

public class RedisCache implements DistributedCache {
  private final RedissonClient redissonClient;
  private final long evictTime;
  private final TimeUnit timeUnit;

  public RedisCache(@NotNull RedisConfig redisConfig, long evictTime, TimeUnit timeUnit) {
    redissonClient = RedisUtils.getClient(redisConfig);
    this.evictTime = evictTime;
    this.timeUnit = timeUnit;
  }

  public <K, V> RMapCache<K, V> getMap(String key) {
    return redissonClient.getMapCache(key);
  }

  public <K, V> void putInsideMap(String key, K innerKey, V value) {
    RMapCache<K, V> cache = redissonClient.getMapCache(key);
    cache.fastPut(innerKey, value, evictTime, timeUnit);
  }

  public static RedisCache of(@NotNull RedisConfig redisConfig, long evictTime, TimeUnit timeUnit) {
    return new RedisCache(redisConfig, evictTime, timeUnit);
  }
}
