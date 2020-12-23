package io.harness.eventsframework.impl.redis;

import static io.harness.eventsframework.impl.redis.RedisUtils.REDIS_STREAM_INTERNAL_KEY;

import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.producer.Message;
import io.harness.redis.RedisConfig;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;

@Slf4j
public class RedisProducer extends AbstractProducer {
  private final RStream<String, String> stream;
  private final RedissonClient redissonClient;

  public RedisProducer(String topicName, @NotNull RedisConfig redisConfig) {
    super(topicName);
    this.redissonClient = RedisUtils.getClient(redisConfig);
    this.stream = RedisUtils.getStream(topicName, redissonClient);
  }

  @Override
  public String send(Message message) throws ProducerShutdownException {
    Map<String, String> redisData = new HashMap<>(message.getMetadataMap());
    redisData.put(REDIS_STREAM_INTERNAL_KEY, Base64.getEncoder().encodeToString(message.getData().toByteArray()));
    try {
      StreamMessageId messageId = stream.addAll(redisData, 10000, false);
      return messageId.toString();
    } catch (RedissonShutdownException e) {
      throw new ProducerShutdownException("Producer for topic: " + getTopicName() + " is shutdown.");
    }
  }

  @Override
  public void shutdown() {
    redissonClient.shutdown();
  }

  public static RedisProducer of(String topicName, @NotNull RedisConfig redisConfig) {
    return new RedisProducer(topicName, redisConfig);
  }
}