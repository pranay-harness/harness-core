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
