package io.harness.eventsframework.impl.redis;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.impl.redis.RedisUtils.REDIS_STREAM_BUILD_VERSION_KEY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.redis.RedisConfig;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class VersionedRedisProducer extends RedisAbstractProducer {
  private final String buildVersion;

  public VersionedRedisProducer(
      String topicName, @NotNull RedisConfig redisConfig, int maxTopicSize, String producerName, String buildVersion) {
    super(topicName, redisConfig, maxTopicSize, producerName);
    this.buildVersion = buildVersion;
  }

  @Override
  protected void populateOtherProducerSpecificData(Map<String, String> redisData) {
    // Populating the git details
    super.populateOtherProducerSpecificData(redisData);
    redisData.put(REDIS_STREAM_BUILD_VERSION_KEY, buildVersion);
  }

  public static VersionedRedisProducer of(String topicName, @NotNull RedisConfig redisConfig, int maxTopicLength,
      String producerName, String buildVersion) {
    return new VersionedRedisProducer(topicName, redisConfig, maxTopicLength, producerName, buildVersion);
  }
}