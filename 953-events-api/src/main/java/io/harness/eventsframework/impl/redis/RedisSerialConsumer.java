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
public class RedisSerialConsumer extends RedisAbstractConsumer {
  public RedisSerialConsumer(
      String topicName, String groupName, String consumerName, RedisConfig redisConfig, Duration maxProcessingTime) {
    super(topicName, groupName, consumerName, redisConfig, maxProcessingTime, 1);
  }

  @Override
  public List<Message> read(Duration maxWaitTime) {
    return getMessages(true, maxWaitTime);
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

  public static RedisSerialConsumer of(String topicName, String groupName, String consumerName,
      @NotNull RedisConfig redisConfig, Duration maxProcessingTime) {
    return new RedisSerialConsumer(topicName, groupName, consumerName, redisConfig, maxProcessingTime);
  }
}
