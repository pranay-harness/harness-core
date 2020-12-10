package io.harness.eventsframework.impl;

import static io.harness.eventsframework.impl.RedisStreamConstants.REDIS_STREAM_INTERNAL_KEY;

import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.redis.RedisConfig;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.client.codec.StringCodec;

@Slf4j
public class RedisProducer extends AbstractProducer {
  private final RedissonClient redissonClient;
  private RStream stream;

  public RedisProducer(String topicName, @NotNull RedisConfig redisConfig) {
    super(topicName);
    this.redissonClient = RedisUtils.getClient(redisConfig);
    this.stream = getStream(topicName);
  }

  @Override
  public String send(Message message) {
    Map<String, String> redisData = new HashMap<>(message.getMetadataMap());
    redisData.put(REDIS_STREAM_INTERNAL_KEY, Base64.getEncoder().encodeToString(message.getData().toByteArray()));
    StreamMessageId messageId = this.stream.addAll(redisData, 10000, false);
    return messageId.toString();
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
    return new StreamMessageId(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
  }
}