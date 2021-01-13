package io.harness.eventsframework.impl.redis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.Long.parseLong;

import io.harness.eventsframework.consumer.Message;
import io.harness.redis.RedisConfig;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.redisson.Redisson;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;

@UtilityClass
public class RedisUtils {
  // Keeping this as small as possible to save on memory for redis instance
  public static final String REDIS_STREAM_INTERNAL_KEY = "o";

  public RedissonClient getClient(RedisConfig redisConfig) {
    Config config = new Config();
    if (!redisConfig.isSentinel()) {
      config.useSingleServer().setAddress(redisConfig.getRedisUrl());
    } else {
      config.useSentinelServers().setMasterName(redisConfig.getMasterName());
      for (String sentinelUrl : redisConfig.getSentinelUrls()) {
        config.useSentinelServers().addSentinelAddress(sentinelUrl);
      }
      config.useSentinelServers().setReadMode(ReadMode.valueOf(redisConfig.getReadMode().name()));
    }
    config.setNettyThreads(redisConfig.getNettyThreads());
    config.setUseScriptCache(redisConfig.isUseScriptCache());
    return Redisson.create(config);
  }

  public Timestamp getMessageTimestamp(String messageId) {
    return fromMillis(parseLong(messageId.split("-")[0]));
  }

  public RStream<String, String> getStream(String topicName, RedissonClient client) {
    return client.getStream(getStreamName(topicName), new StringCodec("UTF-8"));
  }

  public String getStreamName(String topicName) {
    return "streams:" + topicName;
  }

  public StreamMessageId getStreamId(String messageId) {
    if (messageId.equals("$")) {
      return StreamMessageId.NEWEST;
    }

    String[] parts = messageId.split("-");
    return new StreamMessageId(parseLong(parts[0]), parseLong(parts[1]));
  }

  public List<Message> getMessageObject(Map<StreamMessageId, Map<String, String>> result) {
    if (isEmpty(result)) {
      return Collections.emptyList();
    } else {
      List<Message> messages = new ArrayList<>();
      Map<String, String> messageMap;
      StreamMessageId messageId;
      for (Map.Entry<StreamMessageId, Map<String, String>> entry : result.entrySet()) {
        messageId = entry.getKey();
        messageMap = entry.getValue();
        messages.add(getConsumerMessageObject(messageId, messageMap));
      }

      return messages;
    }
  }

  public io.harness.eventsframework.producer.Message getProducedMessage(
      String messageData, Map<String, String> messageMap) {
    return io.harness.eventsframework.producer.Message.newBuilder()
        .setData(ByteString.copyFrom(Base64.getDecoder().decode(messageData)))
        .putAllMetadata(messageMap)
        .build();
  }

  public Message getConsumerMessageObject(StreamMessageId messageId, Map<String, String> messageMap) {
    String messageData = messageMap.remove(REDIS_STREAM_INTERNAL_KEY);

    return Message.newBuilder()
        .setId(messageId.toString())
        .setMessage(getProducedMessage(messageData, messageMap))
        .setTimestamp(RedisUtils.getMessageTimestamp(messageId.toString()))
        .build();
  }
}
