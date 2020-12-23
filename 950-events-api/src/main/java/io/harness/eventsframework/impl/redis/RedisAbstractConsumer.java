package io.harness.eventsframework.impl.redis;

import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.redis.RedisConfig;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;

@Slf4j
public abstract class RedisAbstractConsumer extends AbstractConsumer {
  protected RStream<String, String> stream;
  protected RedissonClient redissonClient;

  public RedisAbstractConsumer(String topicName, String groupName, @NotNull RedisConfig redisConfig) {
    super(topicName, groupName);
    initConsumerGroup(redisConfig);
  }

  public RedisAbstractConsumer(String topicName, String groupName, String consumerName, RedisConfig redisConfig) {
    super(topicName, groupName, consumerName);
    initConsumerGroup(redisConfig);
  }

  private void initConsumerGroup(RedisConfig redisConfig) {
    this.redissonClient = RedisUtils.getClient(redisConfig);
    this.stream = RedisUtils.getStream(getTopicName(), redissonClient);
    createConsumerGroup();
  }

  private void createConsumerGroup() {
    String groupName = getGroupName();
    try {
      stream.createGroup(groupName);
    } catch (RedisException e) {
      log.info("Consumer group {} already exists, continuing with consumer operations...", groupName);
    }
  }

  protected List<Message> getUndeliveredMessages(long maxWaitTime, TimeUnit unit) throws ConsumerShutdownException {
    try {
      return RedisUtils.getMessageObject(stream.readGroup(getGroupName(), getName(), 1, maxWaitTime, unit));
    } catch (RedissonShutdownException e) {
      throw new ConsumerShutdownException("Consumer " + getName() + "is shutdown.");
    }
  }

  @Override
  public void acknowledge(String messageId) throws ConsumerShutdownException {
    try {
      stream.ack(getGroupName(), RedisUtils.getStreamId(messageId));
    } catch (RedissonShutdownException e) {
      throw new ConsumerShutdownException("Consumer " + getName() + "is shutdown.");
    }
  }

  @Override
  public void shutdown() {
    redissonClient.shutdown();
  }
}
