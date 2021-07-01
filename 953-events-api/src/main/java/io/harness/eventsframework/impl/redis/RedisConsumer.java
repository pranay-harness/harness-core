package io.harness.eventsframework.impl.redis;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.redis.RedisConfig;

import java.time.Duration;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

@OwnedBy(PL)
@Slf4j
public class RedisConsumer extends RedisAbstractConsumer {
  public RedisConsumer(
      String topicName, String groupName, @NotNull RedisConfig redisConfig, Duration maxProcessingTime, int batchSize) {
    super(topicName, groupName, redisConfig, maxProcessingTime, batchSize);
  }

  public RedisConsumer(String topicName, String groupName, @NotNull RedissonClient redissonClient,
      Duration maxProcessingTime, int batchSize) {
    super(topicName, groupName, redissonClient, maxProcessingTime, batchSize);
  }

  @Override
  public List<Message> read(Duration maxWaitTime) {
    return getMessages(false, maxWaitTime);
  }

  public static RedisConsumer of(
      String topicName, String groupName, @NotNull RedisConfig redisConfig, Duration maxProcessingTime, int batchSize) {
    return new RedisConsumer(topicName, groupName, redisConfig, maxProcessingTime, batchSize);
  }

  public static RedisConsumer of(String topicName, String groupname, @NotNull RedissonClient redissonClient,
      Duration maxProcessingTime, int batchSize) {
    return new RedisConsumer(topicName, groupname, redissonClient, maxProcessingTime, batchSize);
  }
}
