package io.harness.eventsframework.impl.redis;

import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.redis.RedisConfig;

import java.time.Duration;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisConsumer extends RedisAbstractConsumer {
  public RedisConsumer(
      String topicName, String groupName, @NotNull RedisConfig redisConfig, Duration maxProcessingTime, int batchSize) {
    super(topicName, groupName, redisConfig, maxProcessingTime, batchSize);
  }

  @Override
  public List<Message> read(Duration maxWaitTime) throws ConsumerShutdownException {
    return getMessages(false, maxWaitTime);
  }

  public static RedisConsumer of(
      String topicName, String groupName, @NotNull RedisConfig redisConfig, Duration maxProcessingTime, int batchSize) {
    return new RedisConsumer(topicName, groupName, redisConfig, maxProcessingTime, batchSize);
  }
}
