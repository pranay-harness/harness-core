package io.harness;

import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.entity_crud.account.AccountEntityChangeDTO;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.redis.RedisConfig;

import com.google.common.collect.ImmutableMap;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageProducer implements Runnable {
  private final RedisProducer client;
  private final String color;

  public MessageProducer(String channel, RedisConfig redisConfig, String color) {
    this.client = RedisProducer.of(channel, redisConfig, 10000, "dummyMessageProducer");
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
      Message projectEvent;
      if (count % 3 == 0) {
        projectEvent =
            Message.newBuilder()
                .putAllMetadata(ImmutableMap.of("accountId", String.valueOf(count)))
                .setData(AccountEntityChangeDTO.newBuilder().setAccountId(String.valueOf(count)).build().toByteString())
                .build();
      } else {
        projectEvent =
            Message.newBuilder()
                .putAllMetadata(ImmutableMap.of("accountId", String.valueOf(count)))
                .setData(
                    ProjectEntityChangeDTO.newBuilder().setIdentifier(String.valueOf(count)).build().toByteString())
                .build();
      }

      String messageId = null;
      try {
        messageId = client.send(projectEvent);
        log.info("{}Pushed pid: {} in redis, received: {}{}", color, count, messageId, ColorConstants.TEXT_RESET);
      } catch (EventsFrameworkDownException e) {
        e.printStackTrace();
        log.error("{}Pushing message {} failed due to producer shutdown.{}", color, count, ColorConstants.TEXT_RESET);
        break;
      }

      count += 1;
      TimeUnit.SECONDS.sleep(1);
    }
  }
}