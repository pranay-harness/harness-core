package io.harness;

import io.harness.eventsframework.*;
import io.harness.eventsframework.impl.RedisProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.redis.RedisConfig;

import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

@Slf4j
public class MessageProducer implements Runnable {
  io.harness.eventsframework.impl.RedisProducer client;
  String channel;
  String color;

  public MessageProducer(String channel, RedisConfig redisConfig, String color) {
    this.client = new RedisProducer(channel, redisConfig);
    this.channel = channel;
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
              .putAllMetadata(ImmutableMap.of("accountId", "account1", "desc", "desc1", "test", "test2"))
              .setData(ProjectUpdate.newBuilder().setProjectIdentifier(String.valueOf(count)).build().toByteString())
              .build();

      String messageId = client.send(projectEvent);
      log.info("{}Pushed pid: {} in redis, received: {}{}", color, count, messageId, ColorConstants.TEXT_RESET);

      count += 1;
      Thread.sleep(500);
    }
  }
}