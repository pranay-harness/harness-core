package io.harness.eventsframework.impl.redis;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

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

@OwnedBy(PL)
@Slf4j
public class RedisConsumer extends RedisAbstractConsumer {
  public RedisConsumer(
      String topicName, String groupName, @NotNull RedisConfig redisConfig, Duration maxProcessingTime, int batchSize) {
    super(topicName, groupName, redisConfig, maxProcessingTime, batchSize);
  }

  @Override
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
        messages.add(RedisUtils.getConsumerMessageObject(messageId, messageMap));
      }

      return messages;
    }
  }

  @Override
  public List<Message> read(Duration maxWaitTime) {
    return getMessages(false, maxWaitTime);
  }

  public static RedisConsumer of(
      String topicName, String groupName, @NotNull RedisConfig redisConfig, Duration maxProcessingTime, int batchSize) {
    return new RedisConsumer(topicName, groupName, redisConfig, maxProcessingTime, batchSize);
  }
}
