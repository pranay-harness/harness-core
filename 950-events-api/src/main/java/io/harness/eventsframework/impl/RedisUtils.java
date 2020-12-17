package io.harness.eventsframework.impl;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.Long.parseLong;

import io.harness.redis.RedisConfig;

import com.google.protobuf.Timestamp;
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

  RedissonClient getClient(RedisConfig redisConfig) {
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

  Timestamp getMessageTimestamp(String messageId) {
    return fromMillis(parseLong(messageId.split("-")[0]));
  }

  RStream<String, String> getStream(String topicName, RedissonClient client) {
    return client.getStream(getStreamName(topicName), new StringCodec("UTF-8"));
  }

  String getStreamName(String topicName) {
    return "streams:" + topicName;
  }

  StreamMessageId getStreamId(String messageId) {
    if (messageId.equals("$"))
      return StreamMessageId.NEWEST;
    String[] parts = messageId.split("-");
    return new StreamMessageId(parseLong(parts[0]), parseLong(parts[1]));
  }
}
