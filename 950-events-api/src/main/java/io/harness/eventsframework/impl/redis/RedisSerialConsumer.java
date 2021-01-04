package io.harness.eventsframework.impl.redis;

import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.redis.RedisConfig;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.StreamMessageId;

@Slf4j
public class RedisSerialConsumer extends RedisAbstractConsumer {
  public RedisSerialConsumer(
      String topicName, String groupName, String consumerName, RedisConfig redisConfig, Duration maxProcessingTime) {
    super(topicName, groupName, consumerName, redisConfig, maxProcessingTime);
  }

  private List<Message> getUnackedMessages(long maxWaitTime, TimeUnit unit) throws ConsumerShutdownException {
    try {
      Map<StreamMessageId, Map<String, String>> messages =
          stream.readGroup(getGroupName(), getName(), 1, maxWaitTime, unit, StreamMessageId.ALL);
      return RedisUtils.getMessageObject(messages);
    } catch (RedissonShutdownException e) {
      throw new ConsumerShutdownException("Consumer " + getName() + "is shutdown.");
    }
  }

  @Override
  public List<Message> read(long maxWaitTime, TimeUnit unit) throws ConsumerShutdownException {
    List<Message> unackedMessages = getUnackedMessages(maxWaitTime, unit);

    if (unackedMessages.isEmpty()) {
      return getUndeliveredMessages(maxWaitTime, unit);
    } else {
      return unackedMessages;
    }
  }

  public static RedisSerialConsumer of(String topicName, String groupName, String consumerName,
      @NotNull RedisConfig redisConfig, Duration maxProcessingTime) {
    return new RedisSerialConsumer(topicName, groupName, consumerName, redisConfig, maxProcessingTime);
  }
}
