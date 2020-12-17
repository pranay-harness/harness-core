package io.harness.eventsframework.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eventsframework.impl.RedisUtils.REDIS_STREAM_INTERNAL_KEY;

import io.harness.eventsframework.ConsumerShutdownException;
import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.consumer.Message;

import com.google.protobuf.ByteString;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

  public RedisAbstractConsumer(String topicName, String groupName, RedissonClient redissonClient) {
    super(topicName, groupName);
    initConsumerGroup(redissonClient);
  }

  public RedisAbstractConsumer(String topicName, String groupName, String consumerName, RedissonClient redissonClient) {
    super(topicName, groupName, consumerName);
    initConsumerGroup(redissonClient);
  }

  private void initConsumerGroup(RedissonClient redissonClient) {
    this.stream = RedisUtils.getStream(topicName, redissonClient);
    this.redissonClient = redissonClient;
    this.createConsumerGroup();
  }

  private void createConsumerGroup() {
    try {
      this.stream.createGroup(groupName);
    } catch (RedisException e) {
      log.info("Consumer group {} already exists, continuing with consumer operations...", groupName);
    }
  }

  protected List<Message> getMessageObject(Map<StreamMessageId, Map<String, String>> result) {
    if (isEmpty(result)) {
      return Collections.emptyList();
    } else {
      List<Message> messages = new ArrayList<>();
      Map<String, String> messageMap;
      StreamMessageId messageId;
      for (Map.Entry<StreamMessageId, Map<String, String>> entry : result.entrySet()) {
        messageId = entry.getKey();
        messageMap = entry.getValue();
        messages.add(RedisAbstractConsumer.getConsumerMessageObject(messageId, messageMap));
      }

      return messages;
    }
  }

  protected List<Message> getUndeliveredMessages(long maxWaitTime, TimeUnit unit) throws ConsumerShutdownException {
    try {
      return getMessageObject(this.stream.readGroup(groupName, name, 1, maxWaitTime, unit));
    } catch (RedissonShutdownException e) {
      throw new ConsumerShutdownException("Consumer " + getName() + "is shutdown.");
    }
  }

  private static io.harness.eventsframework.producer.Message getProducedMessage(
      String messageData, Map<String, String> messageMap) {
    return io.harness.eventsframework.producer.Message.newBuilder()
        .setData(ByteString.copyFrom(Base64.getDecoder().decode(messageData)))
        .putAllMetadata(messageMap)
        .build();
  }

  private static Message getConsumerMessageObject(StreamMessageId messageId, Map<String, String> messageMap) {
    String messageData = messageMap.remove(REDIS_STREAM_INTERNAL_KEY);

    return Message.newBuilder()
        .setId(messageId.toString())
        .setMessage(RedisAbstractConsumer.getProducedMessage(messageData, messageMap))
        .setTimestamp(RedisUtils.getMessageTimestamp(messageId.toString()))
        .build();
  }

  @Override
  public void acknowledge(String messageId) throws ConsumerShutdownException {
    try {
      this.stream.ack(groupName, RedisUtils.getStreamId(messageId));
    } catch (RedissonShutdownException e) {
      throw new ConsumerShutdownException("Consumer " + getName() + "is shutdown.");
    }
  }

  @Override
  public void shutdown() {
    this.redissonClient.shutdown();
  }
}
