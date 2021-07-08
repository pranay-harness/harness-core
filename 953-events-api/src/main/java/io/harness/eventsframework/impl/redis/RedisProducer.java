package io.harness.eventsframework.impl.redis;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.redis.RedisConfig;

import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class RedisProducer extends RedisAbstractProducer {
  public RedisProducer(String topicName, @NotNull RedisConfig redisConfig, int maxTopicSize, String producerName) {
    super(topicName, redisConfig, maxTopicSize, producerName);
  }

  public static RedisProducer of(
      String topicName, @NotNull RedisConfig redisConfig, int maxTopicLength, String producerName) {
    return new RedisProducer(topicName, redisConfig, maxTopicLength, producerName);
  }
}