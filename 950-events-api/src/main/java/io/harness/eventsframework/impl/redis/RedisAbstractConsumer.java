package io.harness.eventsframework.impl.redis;

import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.redis.RedisConfig;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.client.RedisException;

@Slf4j
public abstract class RedisAbstractConsumer extends AbstractConsumer {
  protected RStream<String, String> stream;
  protected RedissonClient redissonClient;
  protected Duration maxProcessingTime;

  public RedisAbstractConsumer(
      String topicName, String groupName, @NotNull RedisConfig redisConfig, Duration maxProcessingTime) {
    super(topicName, groupName);
    initConsumerGroup(redisConfig, maxProcessingTime);
  }

  public RedisAbstractConsumer(
      String topicName, String groupName, String consumerName, RedisConfig redisConfig, Duration maxProcessingTime) {
    super(topicName, groupName, consumerName);
    initConsumerGroup(redisConfig, maxProcessingTime);
  }

  private void initConsumerGroup(RedisConfig redisConfig, Duration maxProcessingTime) {
    this.redissonClient = RedisUtils.getClient(redisConfig);
    this.stream = RedisUtils.getStream(getTopicName(), redissonClient);
    this.maxProcessingTime = maxProcessingTime;
    createConsumerGroup();
  }

  private void createConsumerGroup() {
    String groupName = getGroupName();
    try {
      stream.createGroup(groupName, StreamMessageId.ALL);
    } catch (RedisException e) {
      log.info("Consumer group {} already exists, continuing with consumer operations...", groupName);
    }
  }

  protected List<Message> getUndeliveredMessages(long maxWaitTime, TimeUnit unit) throws ConsumerShutdownException {
    while (true) {
      try {
        Map<StreamMessageId, Map<String, String>> result =
            stream.readGroup(getGroupName(), getName(), 1, maxWaitTime, unit);
        return RedisUtils.getMessageObject(result);
      } catch (RedissonShutdownException e) {
        throw new ConsumerShutdownException("Consumer " + getName() + " is shutdown.");
      } catch (RedisException e) {
        log.warn("Consumer " + getName() + " failed getUndeliveredMessages", e);
        waitForRedisToComeUp();
      }
    }
  }

  @Override
  public void acknowledge(String messageId) throws ConsumerShutdownException {
    while (true) {
      try {
        stream.ack(getGroupName(), RedisUtils.getStreamId(messageId));
        return;
      } catch (RedissonShutdownException e) {
        throw new ConsumerShutdownException("Consumer " + getName() + " failed acknowledge - Consumer shutdown.");
      } catch (RedisException e) {
        log.warn("Redis is not up for acknowledge", e);
        waitForRedisToComeUp();
      }
    }
  }

  @Override
  public void shutdown() {
    redissonClient.shutdown();
  }

  protected void waitForRedisToComeUp() {
    try {
      TimeUnit.MILLISECONDS.sleep(500);
    } catch (InterruptedException e) {
      log.error("Polling to redis was interrupted, shutting down consumer", e);
      shutdown();
    }
  }
}
