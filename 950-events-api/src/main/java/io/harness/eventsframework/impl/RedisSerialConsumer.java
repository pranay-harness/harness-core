package io.harness.eventsframework.impl;

import io.harness.eventsframework.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.redis.RedisConfig;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.PendingResult;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;

@Slf4j
public class RedisSerialConsumer extends RedisAbstractConsumer {
  public RedisSerialConsumer(
      String topicName, String groupName, String consumerName, @NotNull RedissonClient redissonClient) {
    super(topicName, groupName, consumerName, redissonClient);
  }

  private List<Message> getUnackedMessages(long maxWaitTime, TimeUnit unit) throws ConsumerShutdownException {
    try {
      Map<StreamMessageId, Map<String, String>> messages =
          this.stream.readGroup(groupName, name, 1, maxWaitTime, unit, StreamMessageId.ALL);
      return getMessageObject(messages);
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

  public static RedisSerialConsumer of(
      String topicName, String groupName, String consumerName, @NotNull RedisConfig redisConfig) {
    RedissonClient client = RedisUtils.getClient(redisConfig);
    return new RedisSerialConsumer(topicName, groupName, consumerName, client);
  }
}
