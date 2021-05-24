package io.harness.ng.core.event;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.eventsframework.EventsFrameworkConstants.SETUP_USAGE;

import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.consumer.Message;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SetupUsageStreamConsumer implements Runnable {
  private final Consumer redisConsumer;
  private final List<MessageListener> messageListenersList;

  @Inject
  public SetupUsageStreamConsumer(@Named(SETUP_USAGE) Consumer redisConsumer,
      @Named(SETUP_USAGE) MessageListener setupUsageChangeEventMessageProcessor) {
    this.redisConsumer = redisConsumer;
    messageListenersList = new ArrayList<>();
    messageListenersList.add(setupUsageChangeEventMessageProcessor);
  }

  @Override
  public void run() {
    log.info("Started the consumer for setup usage stream");
    SecurityContextBuilder.setContext(new ServicePrincipal(NG_MANAGER.getServiceId()));
    try {
      while (!Thread.currentThread().isInterrupted()) {
        pollAndProcessMessages();
      }
    } catch (Exception ex) {
      log.error("Setup Usage consumer unexpectedly stopped", ex);
    }
    SecurityContextBuilder.unsetCompleteContext();
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
