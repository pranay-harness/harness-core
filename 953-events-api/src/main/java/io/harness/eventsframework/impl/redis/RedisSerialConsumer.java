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
public class RedisSerialConsumer extends RedisAbstractConsumer {
  public RedisSerialConsumer(
      String topicName, String groupName, String consumerName, RedisConfig redisConfig, Duration maxProcessingTime) {
    super(topicName, groupName, consumerName, redisConfig, maxProcessingTime, 1);
  }

  public RedisSerialConsumer(String topicName, String groupName, String consumerName, RedissonClient redissonClient,
      Duration maxProcessingTime, String envNamespace) {
    super(topicName, groupName, consumerName, redissonClient, maxProcessingTime, 1, envNamespace);
  }

  @Override
  public List<Message> read(Duration maxWaitTime) {
    return getMessages(true, maxWaitTime);
  }

  public static RedisSerialConsumer of(String topicName, String groupName, String consumerName,
      @NotNull RedisConfig redisConfig, Duration maxProcessingTime) {
    return new RedisSerialConsumer(topicName, groupName, consumerName, redisConfig, maxProcessingTime);
  }

  public static RedisSerialConsumer of(String topicName, String groupName, String consumerName,
      @NotNull RedissonClient redissonClient, Duration maxProcessingTime, String envNamespace) {
    return new RedisSerialConsumer(topicName, groupName, consumerName, redissonClient, maxProcessingTime, envNamespace);
  }
}
