package io.harness;

import io.harness.eventsframework.Event;
import io.harness.eventsframework.ProjectUpdate;
import io.harness.eventsframework.RedisStreamClient;
import io.harness.eventsframework.StreamChannel;

import com.google.protobuf.Any;
import java.util.Base64;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageProducer implements Runnable {
  RedisStreamClient client;
  StreamChannel channel;
  String color;

  public MessageProducer(RedisStreamClient client, StreamChannel channel, String color) {
    this.client = client;
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
      Event projectEvent =
          Event.newBuilder()
              .setAccountId("account1")
              .setPayload(Any.pack(ProjectUpdate.newBuilder().setProjectIdentifier(String.valueOf(count)).build()))
              .build();
      log.info("{}Pushing pid: {} in redis{}", color, count, ColorConstants.TEXT_RESET);
      client.publishEvent(channel, projectEvent);
      count += 1;
      Thread.sleep(500);
    }
  }
}