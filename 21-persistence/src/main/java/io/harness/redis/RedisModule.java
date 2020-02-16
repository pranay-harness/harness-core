package io.harness.redis;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

@Slf4j
public class RedisModule extends AbstractModule {
  @Provides
  public RedissonClient getClient(RedisConfig redisConfig) {
    if (!redisConfig.isEnabled()) {
      return null;
    }
    Config config = new Config();
    if (!redisConfig.isSentinel()) {
      config.useSingleServer().setAddress(redisConfig.getRedisUrl());
    } else {
      config.useSentinelServers().setMasterName(redisConfig.getMasterName());
      for (String sentinelUrl : redisConfig.getSentinelUrls()) {
        config.useSentinelServers().addSentinelAddress(sentinelUrl);
      }
    }
    logger.info("Starting redis client");
    return Redisson.create(config);
  }

  @Override
  protected void configure() {
    // Nothing to configure here.
  }
}
