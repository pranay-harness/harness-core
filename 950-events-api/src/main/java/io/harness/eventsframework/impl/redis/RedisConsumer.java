package io.harness.eventsframework.impl.redis;

import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.redis.RedisConfig;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.PendingResult;
import org.redisson.api.StreamMessageId;
import org.redisson.client.RedisException;

@Slf4j
public class RedisConsumer extends RedisAbstractConsumer {
  public RedisConsumer(
      String topicName, String groupName, @NotNull RedisConfig redisConfig, Duration maxProcessingTime) {
    super(topicName, groupName, redisConfig, maxProcessingTime);
  }

  private List<Message> getUnackedMessages() throws ConsumerShutdownException {
    List<Message> result = Collections.emptyList();
    String groupName = getGroupName();
    while (true) {
      try {
        PendingResult pendingResult = stream.getPendingInfo(groupName);
        if (pendingResult.getTotal() != 0) {
          Map<StreamMessageId, Map<String, String>> messages = stream.claim(
              groupName, getName(), maxProcessingTime.toMillis(), TimeUnit.MILLISECONDS, pendingResult.getLowestId());
          if (messages.size() != 0) {
            // Claim will return the claimed messages after which have been undelivered for a specific time
            result = RedisUtils.getMessageObject(messages);
          }
        }
        return result;
      } catch (RedissonShutdownException e) {
        throw new ConsumerShutdownException("Consumer " + getName() + " is shutdown.");
      } catch (RedisException e) {
        log.warn("Consumer " + getName() + " failed getUnackedMessage", e);
        waitForRedisToComeUp();
      }
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

  public static RedisConsumer of(
      String topicName, String groupName, @NotNull RedisConfig redisConfig, Duration maxProcessingTime) {
    return new RedisConsumer(topicName, groupName, redisConfig, maxProcessingTime);
  }
}
