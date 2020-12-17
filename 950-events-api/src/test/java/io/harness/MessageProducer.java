package io.harness;

import io.harness.eventsframework.ProducerShutdownException;
import io.harness.eventsframework.impl.RedisProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.project.ProjectEntityChangeDTO;
import io.harness.redis.RedisConfig;

import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageProducer implements Runnable {
  private final io.harness.eventsframework.impl.RedisProducer client;
  private final String color;

  public MessageProducer(String channel, RedisConfig redisConfig, String color) {
    this.client = RedisProducer.of(channel, redisConfig);
    this.color = color;
  }

  @SneakyThrows
  @Override
  public void run() {
    publishMessages();
  }

  private void publishMessages() throws InterruptedException {
    int count = 0;
    while (true) {
      Message projectEvent =
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", "account1"))
              .setData(ProjectEntityChangeDTO.newBuilder().setIdentifier(String.valueOf(count)).build().toByteString())
              .build();

      String messageId = null;
      try {
        messageId = client.send(projectEvent);
      } catch (ProducerShutdownException e) {
        e.printStackTrace();
        log.error("{}Pushing message {} failed due to producer shutdown.{}", color, count, ColorConstants.TEXT_RESET);
        break;
      }
      log.info("{}Pushed pid: {} in redis, received: {}{}", color, count, messageId, ColorConstants.TEXT_RESET);

      count += 1;
      Thread.sleep(500);
    }
  }
}