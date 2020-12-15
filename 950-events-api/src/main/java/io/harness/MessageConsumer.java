package io.harness;

import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.impl.RedisConsumer;
import io.harness.eventsframework.project.ProjectEntityChangeDTO;
import io.harness.lock.AcquiredLock;
import io.harness.lock.redis.RedisPersistentLocker;
import io.harness.redis.RedisConfig;

import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Duration;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageConsumer implements Runnable {
  String color;
  String readVia;
  String channel;
  String groupName;
  Integer processingTime;
  RedisConsumer client;
  RedisPersistentLocker redisLocker;

  public MessageConsumer(String readVia, String channel, String color) {
    this.readVia = readVia;
    this.channel = channel;
    this.color = color;
  }

  public MessageConsumer(
      String readVia, RedisConfig redisConfig, String channel, String groupName, Integer processingTime, String color) {
    this(readVia, channel, color);
    this.client = new RedisConsumer(channel, groupName, redisConfig);
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
    Optional<Message> message;
    while (true) {
      message = client.read();
      processMessage(message);
    }
  }

  private void readViaSerialConsumerGroups() throws InterruptedException, InvalidProtocolBufferException {
    Optional<Message> message;
    AcquiredLock lock;
    while (true) {
      lock = this.redisLocker.tryToAcquireLock(groupName, Duration.ofMinutes(1));
      if (lock == null) {
        Thread.sleep(1000);
        continue;
      }
      message = client.read();
      processMessage(message);
      redisLocker.destroy(lock);

      Thread.sleep(200);
    }
  }

  private void processMessage(Optional<Message> message) throws InterruptedException, InvalidProtocolBufferException {
    if (message.isPresent()) {
      Message actualMessage = message.get();
      ProjectEntityChangeDTO p = ProjectEntityChangeDTO.parseFrom(actualMessage.getMessage().getData());
      log.info("{}Reading messageId: {} for Consumer - {} - pid: {}{}", color, actualMessage.getId(),
          this.client.getName(), p.getIdentifier(), ColorConstants.TEXT_RESET);
      Thread.sleep(processingTime);
      log.info("{}Done processing for Consumer - {} - pid: {}{}", color, this.client.getName(), p.getIdentifier(),
          ColorConstants.TEXT_RESET);
      client.acknowledge(actualMessage.getId());
    }
  }
}
