package io.harness.execution.consumers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.events.PmsEventFrameworkConstants.SDK_RESPONSE_EVENT_LISTENER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.consumer.Message;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.events.PmsEventFrameworkConstants;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class SdkResponseEventRedisConsumer implements Runnable {
  private final Consumer redisConsumer;
  private final List<MessageListener> messageListenersList;

  @Inject
  public SdkResponseEventRedisConsumer(
      @Named(PmsEventFrameworkConstants.SDK_RESPONSE_EVENT_CONSUMER) Consumer redisConsumer,
      @Named(SDK_RESPONSE_EVENT_LISTENER) MessageListener sdkResponseMessageListener) {
    this.redisConsumer = redisConsumer;
    messageListenersList = new ArrayList<>();
    messageListenersList.add(sdkResponseMessageListener);
  }

  @Override
  public void run() {
    log.info("Started the Consumer for SdkResponse Events");
    while (!Thread.currentThread().isInterrupted()) {
      try {
        pollAndProcessMessages();
      } catch (Exception ex) {
        log.error("SdkResponseEvents consumer unexpectedly stopped", ex);
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
    messageListenersList.forEach(messageListener -> {
      if (!messageListener.handleMessage(message)) {
        success.set(false);
      }
    });
    return success.get();
  }
}
