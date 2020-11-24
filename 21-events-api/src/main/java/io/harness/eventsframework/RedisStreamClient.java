package io.harness.eventsframework;

import io.harness.redis.RedisConfig;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;

@Slf4j
public class RedisStreamClient implements EventDrivenClient {
  // Keeping this as small as possible
  private static final String REDIS_STREAM_INTERNAL_KEY = "o";
  private final RedissonClient redissonClient;
  private final long redisBlockTime;

  @Inject
  public RedisStreamClient(@NotNull RedisConfig redisConfig) {
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

    this.redissonClient = Redisson.create(config);
    // @Todo(Raj): This is not respected
    this.redisBlockTime = 200L;
  }

  @Override
  public boolean createConsumerGroup(StreamChannel channel, String groupName) {
    try {
      getStream(channel).createGroup(groupName);
      return true;
    } catch (RedisException e) {
      log.error("Error creating consumerGroup " + groupName, e);
      return false;
    }
  }

  @Override
  public void publishEvent(StreamChannel channel, Event event) {
    getStream(channel).addAll(
        ImmutableMap.of(REDIS_STREAM_INTERNAL_KEY, Base64.getEncoder().encodeToString(event.toByteArray())), 100000,
        false);
  }

  @Override
  public Map<String, Event> readEvent(StreamChannel channel) {
    Map<StreamMessageId, Map<String, String>> redisVal =
        getStream(channel).read(1, this.redisBlockTime, TimeUnit.MILLISECONDS, StreamMessageId.NEWEST);
    return createEventMap(redisVal);
  }

  @Override
  public Map<String, Event> readEvent(StreamChannel channel, String lastId) {
    Map<StreamMessageId, Map<String, String>> redisVal =
        getStream(channel).read(1, this.redisBlockTime, TimeUnit.MILLISECONDS, getStreamId(lastId));
    return createEventMap(redisVal);
  }

  @Override
  public Map<String, Event> readEvent(StreamChannel channel, String groupName, String consumerName) {
    // This performs XREADGROUP with id ">" for a particular consumer in a consumer group
    Map<StreamMessageId, Map<String, String>> redisVal =
        getStream(channel).readGroup(groupName, consumerName, 1, this.redisBlockTime, TimeUnit.MILLISECONDS);
    return createEventMap(redisVal);
  }

  @Override
  public Map<String, Event> readEvent(StreamChannel channel, String groupName, String consumerName, String lastId) {
    Map<StreamMessageId, Map<String, String>> redisVal = getStream(channel).readGroup(
        groupName, consumerName, 1, this.redisBlockTime, TimeUnit.MILLISECONDS, getStreamId(lastId));
    return createEventMap(redisVal);
  }

  @Override
  public void acknowledge(StreamChannel channel, String groupName, String messageId) {
    getStream(channel).ack(groupName, getStreamId(messageId));
  }

  private Map<String, Event> createEventMap(Map<StreamMessageId, Map<String, String>> redisVal) {
    if (redisVal != null && redisVal.size() != 0) {
      return redisVal.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> {
        try {
          return Event.parseFrom(Base64.getDecoder().decode(entry.getValue().get(REDIS_STREAM_INTERNAL_KEY)));
        } catch (InvalidProtocolBufferException e) {
          log.error("Protobuf parsing failed for redis stream - " + entry.getValue(), e);
          return Event.newBuilder().build();
        }
      }));
    } else {
      return Collections.emptyMap();
    }
  }

  private RStream getStream(StreamChannel channel) {
    return this.redissonClient.getStream(getStreamName(channel), new StringCodec("UTF-8"));
  }

  private String getStreamName(StreamChannel channel) {
    return "streams:" + channel.name().toLowerCase();
  }

  private StreamMessageId getStreamId(String messageId) {
    if (messageId == "$")
      return StreamMessageId.NEWEST;
    String[] parts = messageId.split("-");
    return new StreamMessageId(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
  }
}
