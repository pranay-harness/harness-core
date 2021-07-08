package io.harness.eventsframework.impl.redis;

import static io.harness.eventsframework.impl.redis.RedisUtils.REDIS_STREAM_INTERNAL_KEY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.producer.Message;
import io.harness.redis.RedisConfig;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public abstract class RedisAbstractProducer extends AbstractProducer {
  private static final String PRODUCER = "producer";
  private final RStream<String, String> stream;
  private final RedissonClient redissonClient;

  // This is used when the consumer for the event are no longer accepting due to some failure and
  // the messages are continuously being accumulated in Redis. To come up with this number, it is
  // very important to understand the alerting on the consumers and the scale estimations of a
  // particular use-case which is pushing to the topic
  private final int maxTopicSize;

  private final Retry retry;

  public RedisAbstractProducer(
      String topicName, @NotNull RedisConfig redisConfig, int maxTopicSize, String producerName) {
    super(topicName, producerName);
    this.maxTopicSize = maxTopicSize;
    this.redissonClient = io.harness.eventsframework.impl.redis.RedisUtils.getClient(redisConfig);
    this.stream = io.harness.eventsframework.impl.redis.RedisUtils.getStream(
        topicName, redissonClient, redisConfig.getEnvNamespace());
    RetryConfig retryConfig =
        RetryConfig.custom().intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 1.5)).maxAttempts(6).build();

    this.retry = Retry.of("redisProducer:" + topicName, retryConfig);
  }

  @Override
  public String send(Message message) {
    return handleMessage(message);
  }

  private String sendInternal(Message message) {
    Map<String, String> redisData = new HashMap<>(message.getMetadataMap());
    redisData.put(REDIS_STREAM_INTERNAL_KEY, Base64.getEncoder().encodeToString(message.getData().toByteArray()));
    populateOtherProducerSpecificData(redisData);
    log.info("adding message to events stream {}", message.getMetadataMap());
    StreamMessageId messageId = stream.addAll(redisData, maxTopicSize, false);
    return messageId.toString();
  }

  protected void populateOtherProducerSpecificData(Map<String, String> redisData) {
    redisData.put(PRODUCER, this.getProducerName());
  }

  private String handleMessage(Message message) {
    Supplier<String> sendMessageSupplier = () -> sendInternal(message);

    Supplier<String> retryingSendMessage = Retry.decorateSupplier(retry, sendMessageSupplier);

    return Try.ofSupplier(retryingSendMessage)
        .recover(throwable -> {
          // Exhausted exponential backoff to try operating on redis
          throw new EventsFrameworkDownException(throwable.getMessage());
        })
        .get();
  }

  @Override
  public void shutdown() {
    redissonClient.shutdown();
  }
}
