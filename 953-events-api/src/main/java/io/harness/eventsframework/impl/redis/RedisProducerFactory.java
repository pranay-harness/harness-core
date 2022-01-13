/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.eventsframework.impl.redis;

import io.harness.eventsframework.impl.redis.monitoring.publisher.RedisEventMetricPublisher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.constraints.NotNull;
import org.redisson.api.RedissonClient;

@Singleton
public class RedisProducerFactory {
  @Inject RedisEventMetricPublisher redisEventMetricPublisher;

  public RedisProducer createRedisProducer(String topicName, @NotNull RedissonClient redissonClient, int maxTopicSize,
      String producerName, String envNamespace) {
    return new RedisProducer(
        topicName, redissonClient, maxTopicSize, producerName, envNamespace, redisEventMetricPublisher);
  }
}
