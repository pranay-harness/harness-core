package io.harness.eventsframework.impl.redis;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.redis.RedisConfig;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.PendingEntry;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.client.RedisException;

@OwnedBy(PL)
@Slf4j
public abstract class RedisAbstractConsumer extends AbstractConsumer {
  protected RStream<String, String> stream;
  protected RStream<String, String> deadLetterQueue;
  protected RedissonClient redissonClient;
  protected Duration maxProcessingTime;
  protected int batchSize;

  public RedisAbstractConsumer(
      String topicName, String groupName, @NotNull RedisConfig redisConfig, Duration maxProcessingTime, int batchSize) {
    super(topicName, groupName);
    initConsumerGroup(redisConfig, maxProcessingTime, batchSize);
  }

  public RedisAbstractConsumer(String topicName, String groupName, String consumerName, RedisConfig redisConfig,
      Duration maxProcessingTime, int batchSize) {
    super(topicName, groupName, consumerName);
    initConsumerGroup(redisConfig, maxProcessingTime, batchSize);
  }

  private void initConsumerGroup(RedisConfig redisConfig, Duration maxProcessingTime, int batchSize) {
    this.redissonClient = RedisUtils.getClient(redisConfig);
    this.stream = RedisUtils.getStream(getTopicName(), redissonClient, redisConfig.getEnvNamespace());
    this.deadLetterQueue =
        RedisUtils.getDeadLetterStream(getTopicName(), redissonClient, redisConfig.getEnvNamespace());
    this.maxProcessingTime = maxProcessingTime;
    this.batchSize = batchSize;
    createConsumerGroup();
  }

  private void createConsumerGroup() {
    String groupName = getGroupName();
    try {
      stream.createGroup(groupName, StreamMessageId.ALL);
    } catch (RedisException e) {
      log.info("Consumer group {} already exists, continuing with consumer operations...", groupName);
    }
  }

  private void moveMessageToDeadLetterQueue(
      StreamMessageId messageId, String groupName, Map<StreamMessageId, Map<String, String>> messages) {
    Map<String, String> messageData = messages.get(messageId);
    messageData.put("prevMessageId", messageId.toString());
    deadLetterQueue.addAll(messageData, RedisUtils.MAX_DEAD_LETTER_QUEUE_SIZE, false);
    stream.ack(groupName, messageId);
    log.warn("Pushed {} to dead letter queue as max retries exceeded. Message data: {}", messageId, messageData);
  }

  private List<PendingEntry> getPendingEntries() throws ConsumerShutdownException {
    while (true) {
      try {
        return stream.listPending(getGroupName(), StreamMessageId.MIN, StreamMessageId.MAX, batchSize);
      } catch (RedissonShutdownException e) {
        throw new ConsumerShutdownException("Consumer " + getName() + " is shutdown.");
      } catch (RedisException e) {
        log.warn("Consumer " + getName() + " failed getPendingDetails", e);
        waitForRedisToComeUp();
      }
    }
  }

  private List<Message> claimEntries(List<PendingEntry> pendingEntries) throws ConsumerShutdownException {
    String groupName = getGroupName();
    while (true) {
      try {
        if (pendingEntries.isEmpty()) {
          return Collections.emptyList();
        } else {
          StreamMessageId[] messageIds =
              pendingEntries.stream().map(PendingEntry::getId).toArray(StreamMessageId[] ::new);
          Map<StreamMessageId, Map<String, String>> messages =
              stream.claim(groupName, getName(), maxProcessingTime.toMillis(), TimeUnit.MILLISECONDS, messageIds);

          for (PendingEntry entry : pendingEntries) {
            StreamMessageId messageId = entry.getId();
            if (messages.containsKey(messageId) && entry.getLastTimeDelivered() >= RedisUtils.UNACKED_RETRY_COUNT) {
              moveMessageToDeadLetterQueue(messageId, groupName, messages);
            }
          }
          return RedisUtils.getMessageObject(messages);
        }
      } catch (RedissonShutdownException e) {
        throw new ConsumerShutdownException("Consumer " + getName() + " is shutdown.");
      } catch (RedisException e) {
        log.warn("Consumer " + getName() + " failed getPendingDetails", e);
        waitForRedisToComeUp();
      }
    }
  }

  private List<Message> getNewMessages(Duration maxWaitTime) throws ConsumerShutdownException {
    while (true) {
      try {
        Map<StreamMessageId, Map<String, String>> result =
            stream.readGroup(getGroupName(), getName(), batchSize, maxWaitTime.toMillis(), TimeUnit.MILLISECONDS);
        return RedisUtils.getMessageObject(result);
      } catch (RedissonShutdownException e) {
        throw new ConsumerShutdownException("Consumer " + getName() + " is shutdown.");
      } catch (RedisException e) {
        log.warn("Consumer " + getName() + " failed getNewMessages", e);
        waitForRedisToComeUp();
      }
    }
  }

  protected List<Message> getMessages(boolean processUnackedMessagesBeforeNewMessages, Duration maxWaitTime)
      throws ConsumerShutdownException {
    List<PendingEntry> pendingEntries = getPendingEntries();
    if (pendingEntries.isEmpty()) {
      return getNewMessages(maxWaitTime);
    }

    List<Message> claimedMessages = claimEntries(pendingEntries);
    if (!claimedMessages.isEmpty()) {
      return claimedMessages;
    }

    if (processUnackedMessagesBeforeNewMessages) {
      return Collections.emptyList();
    } else {
      return getNewMessages(maxWaitTime);
    }
  }

  @Override
  public void acknowledge(String messageId) throws ConsumerShutdownException {
    while (true) {
      try {
        stream.ack(getGroupName(), RedisUtils.getStreamId(messageId));
        return;
      } catch (RedissonShutdownException e) {
        throw new ConsumerShutdownException("Consumer " + getName() + " failed acknowledge - Consumer shutdown.");
      } catch (RedisException e) {
        log.warn("Redis is not up for acknowledge", e);
        waitForRedisToComeUp();
      }
    }
  }

  @Override
  public void shutdown() {
    redissonClient.shutdown();
  }

  protected void waitForRedisToComeUp() {
    try {
      TimeUnit.MILLISECONDS.sleep(500);
    } catch (InterruptedException e) {
      log.error("Polling to redis was interrupted, shutting down consumer", e);
      shutdown();
    }
  }
}
