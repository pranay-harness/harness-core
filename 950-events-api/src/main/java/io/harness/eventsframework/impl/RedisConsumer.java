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
public class RedisConsumer extends RedisAbstractConsumer {
  public RedisConsumer(String topicName, String groupName, @NotNull RedissonClient redissonClient) {
    super(topicName, groupName, redissonClient);
  }

  private List<Message> getUnackedMessages() throws ConsumerShutdownException {
    List<Message> result = Collections.emptyList();
    try {
      PendingResult pendingResult = this.stream.getPendingInfo(groupName);
      if (pendingResult.getTotal() != 0) {
        Map<StreamMessageId, Map<String, String>> messages =
            this.stream.claim(groupName, name, 10, TimeUnit.MINUTES, pendingResult.getLowestId());
        if (messages.size() != 0)
          // Claim will return the claimed messages after which have been undelivered for a specific time
          result = getMessageObject(messages);
      }
      return result;
    } catch (RedissonShutdownException e) {
      throw new ConsumerShutdownException("Consumer " + getName() + "is shutdown.");
    }
  }

  @Override
  public List<Message> read(long maxWaitTime, TimeUnit unit) throws ConsumerShutdownException {
    List<Message> unackedMessages = getUnackedMessages();
    if (unackedMessages.isEmpty()) {
      return getUndeliveredMessages(maxWaitTime, unit);
    } else {
      return unackedMessages;
    }
  }

  public static RedisConsumer of(String topicName, String groupName, @NotNull RedisConfig redisConfig) {
    RedissonClient client = RedisUtils.getClient(redisConfig);
    return new RedisConsumer(topicName, groupName, client);
  }
}
