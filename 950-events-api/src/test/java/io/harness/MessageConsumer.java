package io.harness;

import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.eventsframework.impl.redis.RedisSerialConsumer;
import io.harness.lock.AcquiredLock;
import io.harness.lock.redis.RedisPersistentLocker;
import io.harness.redis.RedisConfig;

import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageConsumer implements Runnable {
  private final String color;
  private final String readVia;
  private final String channel;
  private final String groupName;
  private final Integer processingTime;
  private final AbstractConsumer client;
  private RedisPersistentLocker redisLocker;

  public MessageConsumer(
      String readVia, RedisConfig redisConfig, String channel, String groupName, Integer processingTime, String color) {
    this.readVia = readVia;
    this.channel = channel;
    this.color = color;
    if (readVia.equals("serialConsumerGroups"))
      this.client =
          RedisSerialConsumer.of(channel, groupName, "hardcodedconsumer", redisConfig, Duration.ofMinutes(10));
    else
      this.client = RedisConsumer.of(channel, groupName, redisConfig, Duration.ofMinutes(10));
    this.groupName = groupName;
    this.processingTime = processingTime;
  }

  public MessageConsumer(RedisPersistentLocker redisLocker, RedisConfig redisConfig, String readVia, String channel,
      String groupName, Integer processingTime, String color) {
    this(readVia, redisConfig, channel, groupName, processingTime, color);
    this.redisLocker = redisLocker;
  }

  @SneakyThrows
  @Override
  public void run() {
    if (this.readVia.equals("consumerGroups")) {
      readViaConsumerGroups();
    } else if (this.readVia.equals("serialConsumerGroups")) {
      readViaSerialConsumerGroups();
    }
  }

  private void readViaConsumerGroups() throws InterruptedException, InvalidProtocolBufferException {
    List<Message> messages;
    while (true) {
      try {
        messages = client.read(2, TimeUnit.SECONDS);
      } catch (ConsumerShutdownException e) {
        e.printStackTrace();
        break;
      }
      for (Message message : messages) {
        processMessage(message);
      }
    }
  }

  private void readViaSerialConsumerGroups() throws InterruptedException, InvalidProtocolBufferException {
    AcquiredLock lock;
    List<Message> messages;
    while (true) {
      lock = this.redisLocker.tryToAcquireLock(groupName, Duration.ofMinutes(1));
      if (lock == null) {
        Thread.sleep(1000);
        continue;
      }

      try {
        messages = client.read(2, TimeUnit.SECONDS);
      } catch (ConsumerShutdownException e) {
        e.printStackTrace();
        break;
      }
      for (Message message : messages) {
        processMessage(message);
      }
      redisLocker.destroy(lock);

      Thread.sleep(200);
    }
  }

  private void processMessage(Message message) throws InterruptedException, InvalidProtocolBufferException {
    ProjectEntityChangeDTO p = ProjectEntityChangeDTO.parseFrom(message.getMessage().getData());
    log.info("{}Reading messageId: {} for Consumer - {} - pid: {}{}", color, message.getId(), this.client.getName(),
        p.getIdentifier(), ColorConstants.TEXT_RESET);
    Thread.sleep(processingTime);
    log.info("{}Done processing for Consumer - {} - pid: {}{}", color, this.client.getName(), p.getIdentifier(),
        ColorConstants.TEXT_RESET);
    try {
      client.acknowledge(message.getId());
    } catch (ConsumerShutdownException e) {
      e.printStackTrace();
    }
  }
}
