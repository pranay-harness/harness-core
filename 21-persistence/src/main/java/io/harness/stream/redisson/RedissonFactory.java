/*
 * Copyright 2008-2020 Async-IO.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.harness.stream.redisson;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.redis.RedisConfig;
import io.harness.redis.RedissonKryoCodec;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereServlet;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;

@OwnedBy(PL)
@UtilityClass
@Slf4j
public class RedissonFactory {
  private static final String REDIS_TYPE = RedissonBroadcaster.class.getName() + ".type";
  private static final String REDIS_SERVER = RedissonBroadcaster.class.getName() + ".server";
  private static final String REDIS_SENTINELS = RedissonBroadcaster.class.getName() + ".sentinels";
  private static final String REDIS_SENTINEL_MASTER_NAME = RedissonBroadcaster.class.getName() + ".master.name";
  static final String REDIS_ENV_NAMESPACE = RedissonBroadcaster.class.getName() + ".env.namespace";
  private static final String REDIS_READ_MODE = RedissonBroadcaster.class.getName() + ".read.mode";
  private static final String REDIS_NETTY_THREADS = RedissonBroadcaster.class.getName() + ".netty.threads";
  private static final String USE_SCRIPT_CACHE = RedissonBroadcaster.class.getName() + ".script.cache";
  private enum RedisType { SINGLE, SENTINEL }

  static RedissonClient getRedissonClient(AtmosphereConfig config) {
    RedisType redisType = RedisType.valueOf(config.getServletConfig().getInitParameter(REDIS_TYPE));
    Config redissonConfig = new Config();
    if (redisType != RedisType.SENTINEL) {
      redissonConfig.useSingleServer().setAddress(config.getServletConfig().getInitParameter(REDIS_SERVER));
      redissonConfig.useSingleServer().setDatabase(1);
    } else {
      List<String> sentinelAddresses =
          Arrays.stream(config.getServletConfig().getInitParameter(REDIS_SENTINELS).split("\\s*,\\s*"))
              .collect(Collectors.toList());
      String masterName = config.getServletConfig().getInitParameter(REDIS_SENTINEL_MASTER_NAME);
      redissonConfig.useSentinelServers().setMasterName(masterName);
      for (String sentinelAddress : sentinelAddresses) {
        redissonConfig.useSentinelServers().addSentinelAddress(sentinelAddress);
      }
      ReadMode readMode = ReadMode.valueOf(config.getServletConfig().getInitParameter(REDIS_READ_MODE));
      redissonConfig.useSentinelServers().setReadMode(readMode);
    }
    redissonConfig.setCodec(new RedissonKryoCodec());
    redissonConfig.setNettyThreads(Integer.valueOf(config.getServletConfig().getInitParameter(REDIS_NETTY_THREADS)));
    redissonConfig.setUseScriptCache(Boolean.valueOf(config.getServletConfig().getInitParameter(USE_SCRIPT_CACHE)));

    try {
      return Redisson.create(redissonConfig);
    } catch (Exception e) {
      throw new UnexpectedException("Could not create a redis client", e);
    }
  }

  public static void setInitParameters(AtmosphereServlet atmosphereServlet, RedisConfig redisConfig) {
    if (!redisConfig.isSentinel()) {
      atmosphereServlet.framework()
          .addInitParameter(REDIS_TYPE, RedisType.SINGLE.toString())
          .addInitParameter(REDIS_SERVER, redisConfig.getRedisUrl());
    } else {
      atmosphereServlet.framework()
          .addInitParameter(REDIS_TYPE, RedisType.SENTINEL.toString())
          .addInitParameter(REDIS_SENTINEL_MASTER_NAME, redisConfig.getMasterName())
          .addInitParameter(REDIS_SENTINELS, String.join(",", redisConfig.getSentinelUrls()))
          .addInitParameter(REDIS_READ_MODE, redisConfig.getReadMode().name());
    }
    atmosphereServlet.framework()
        .addInitParameter(REDIS_ENV_NAMESPACE, redisConfig.getEnvNamespace())
        .addInitParameter(REDIS_NETTY_THREADS, String.valueOf(redisConfig.getNettyThreads()))
        .addInitParameter(USE_SCRIPT_CACHE, String.valueOf(redisConfig.isUseScriptCache()));
  }
}
