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
public class MessagePublisher implements Runnable {
  RedisStreamClient client;
  StreamChannel channel;

  public MessagePublisher(RedisStreamClient client, StreamChannel channel) {
    this.client = client;
    this.channel = channel;
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
              .setPayload(Any.pack(ProjectUpdate.newBuilder().setProjectId(String.valueOf(count)).build()))
              .build();
      log.info("\u001B[33m"
          + "pushing " + Base64.getEncoder().encodeToString(projectEvent.toByteArray()) + " in redis"
          + "\u001B[0m");
      client.publishEvent(channel, projectEvent);
      count += 1;
      Thread.sleep(500);
    }
  }
}