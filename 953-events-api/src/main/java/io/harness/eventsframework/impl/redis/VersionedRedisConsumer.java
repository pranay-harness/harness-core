package io.harness.eventsframework.impl.redis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eventsframework.impl.redis.RedisUtils.REDIS_STREAM_BUILD_VERSION_KEY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.redis.RedisConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.StreamMessageId;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class VersionedRedisConsumer extends RedisAbstractConsumer {
  private final String buildVersion;

  public VersionedRedisConsumer(String topicName, String groupName, @NotNull RedisConfig redisConfig,
      Duration maxProcessingTime, int batchSize, String buildVersion) {
    super(topicName, groupName, redisConfig, maxProcessingTime, batchSize);
    this.buildVersion = buildVersion;
  }

  @Override
  protected List<Message> getMessageObject(Map<StreamMessageId, Map<String, String>> result) {
    if (isEmpty(result)) {
      return Collections.emptyList();
    } else {
      List<Message> messages = new ArrayList<>();
      Map<String, String> messageMap;
      StreamMessageId messageId;
      String messageBuildVersion;
      for (Map.Entry<StreamMessageId, Map<String, String>> entry : result.entrySet()) {
        messageId = entry.getKey();
        messageMap = entry.getValue();
        messageBuildVersion = messageMap.get(REDIS_STREAM_BUILD_VERSION_KEY);
        if (isEmpty(messageBuildVersion) || (Float.parseFloat(buildVersion) >= Float.parseFloat(messageBuildVersion))) {
          log.info("Reading {} {}: message version {} and consumer build version {}", messageId, messageMap,
              messageBuildVersion, buildVersion);
          messages.add(RedisUtils.getConsumerMessageObject(messageId, messageMap));
        } else {
          log.info("Not reading {} {}: message version {} and consumer build version {}", messageId, messageMap,
              messageBuildVersion, buildVersion);
        }
      }

      return messages;
    }
  }

  @Override
  public List<Message> read(Duration maxWaitTime) {
    return getMessages(false, maxWaitTime);
  }

  public static VersionedRedisConsumer of(String topicName, String groupName, @NotNull RedisConfig redisConfig,
      Duration maxProcessingTime, int batchSize, String buildVersion) {
    return new VersionedRedisConsumer(topicName, groupName, redisConfig, maxProcessingTime, batchSize, buildVersion);
  }
}
