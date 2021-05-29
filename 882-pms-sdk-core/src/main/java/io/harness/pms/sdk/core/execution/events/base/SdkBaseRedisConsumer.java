package io.harness.pms.sdk.core.execution.events.base;

import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.consumer.Message;
import io.harness.ng.core.event.MessageListener;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SdkBaseRedisConsumer implements Runnable {
  private final Consumer redisConsumer;
  private final MessageListener messageListener;

  public SdkBaseRedisConsumer(Consumer redisConsumer, MessageListener messageListener) {
    this.redisConsumer = redisConsumer;
    this.messageListener = messageListener;
  }

  @Override
  public void run() {
    log.info("Started the Consumer {}", this.getClass().getSimpleName());
    while (!Thread.currentThread().isInterrupted()) {
      try {
        pollAndProcessMessages();
      } catch (Exception ex) {
        log.error("Consumer {} unexpectedly stopped", this.getClass().getSimpleName(), ex);
      }
    }
  }

  private void pollAndProcessMessages() {
    List<Message> messages;
    String messageId;
    boolean messageProcessed;
    messages = redisConsumer.read(Duration.ofSeconds(10));
    for (Message message : messages) {
      messageId = message.getId();
      messageProcessed = handleMessage(message);
      if (messageProcessed) {
        redisConsumer.acknowledge(messageId);
      }
    }
  }

  private boolean handleMessage(Message message) {
    try {
      return processMessage(message);
    } catch (Exception ex) {
      // This is not evicted from events framework so that it can be processed
      // by other consumer if the error is a runtime error
      log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
      return false;
    }
  }

  private boolean processMessage(Message message) {
    AtomicBoolean success = new AtomicBoolean(true);
    if (!messageListener.handleMessage(message)) {
      success.set(false);
    }
    return success.get();
  }
}
