package io.harness.eventsframework.impl;

import static io.harness.eventsframework.impl.RedisStreamConstants.REDIS_STREAM_INTERNAL_KEY;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.Long.parseLong;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.consumer.Message;
import io.harness.redis.RedisConfig;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;

@Slf4j
public class RedisConsumer extends AbstractConsumer {
  private RStream stream;
  private long redisBlockTime;
  private final RedissonClient redissonClient;

  public RedisConsumer(String topicName, String groupName, @NotNull RedisConfig redisConfig) {
    super(topicName, groupName);
    this.redissonClient = RedisUtils.getClient(redisConfig);
    this.stream = getStream(topicName);
    this.redisBlockTime = 1000L;
    this.createConsumerGroup();
  }

  @Override
  public Optional<Message> read() {
    Map<StreamMessageId, Map<String, String>> result =
        this.stream.readGroup(groupName, name, 1, this.redisBlockTime, TimeUnit.MILLISECONDS);
    return Optional.ofNullable(getMessageObject(result));
  }

  private void createConsumerGroup() {
    try {
      this.stream.createGroup(groupName);
    } catch (RedisException e) {
      log.info("Consumer group {} already exists, continuing with consumer operations...", groupName);
    }
  }

  private Message getMessageObject(Map<StreamMessageId, Map<String, String>> result) {
    if (result.isEmpty()) {
      return null;
    } else {
      StreamMessageId messageId = result.keySet().iterator().next();

      Map<String, String> messageMap = result.get(messageId);
      String messageData = (String) messageMap.remove(REDIS_STREAM_INTERNAL_KEY);
      io.harness.eventsframework.producer.Message producedMessage =
          io.harness.eventsframework.producer.Message.newBuilder()
              .setData(ByteString.copyFrom(Base64.getDecoder().decode(messageData)))
              .putAllMetadata(messageMap)
              .build();

      return Message.newBuilder()
          .setId(messageId.toString())
          .setMessage(producedMessage)
          .setTimestamp(getMessageTimestamp(messageId.toString()))
          .build();
    }
  }

  private Timestamp getMessageTimestamp(String messageId) {
    return fromMillis(parseLong(messageId.split("-")[0]));
  }

  private RStream getStream(String topicName) {
    return this.redissonClient.getStream(getStreamName(topicName), new StringCodec("UTF-8"));
  }

  private String getStreamName(String topicName) {
    return "streams:" + topicName;
  }

  private StreamMessageId getStreamId(String messageId) {
    if (messageId.equals("$"))
      return StreamMessageId.NEWEST;
    String[] parts = messageId.split("-");
    return new StreamMessageId(parseLong(parts[0]), parseLong(parts[1]));
  }

  @Override
  public void acknowledge(String messageId) {
    this.stream.ack(groupName, getStreamId(messageId));
  }
}
