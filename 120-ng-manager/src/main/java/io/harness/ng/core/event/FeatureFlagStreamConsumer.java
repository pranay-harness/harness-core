package io.harness.ng.core.event;

import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class FeatureFlagStreamConsumer implements Runnable {
  private final Consumer eventConsumer;
  private final MessageProcessor featureFlagChangeEventMessageProcessor;

  @Inject
  public FeatureFlagStreamConsumer(@Named(EventsFrameworkConstants.FEATURE_FLAG_STREAM) Consumer eventConsumer,
      @Named(EventsFrameworkConstants.FEATURE_FLAG_STREAM) MessageProcessor featureFlagChangeEventMessageProcessor) {
    this.eventConsumer = eventConsumer;
    this.featureFlagChangeEventMessageProcessor = featureFlagChangeEventMessageProcessor;
  }

  @Override
  public void run() {
    log.info("Started the consumer for feature flag stream");
    try {
      while (!Thread.currentThread().isInterrupted()) {
        pollAndProcessMessages();
      }
    } catch (Exception ex) {
      log.error("Feature flag stream consumer unexpectedly stopped", ex);
    }
  }

  private void pollAndProcessMessages() throws ConsumerShutdownException {
    String messageId;
    boolean messageProcessed;
    List<Message> messages;
    messages = eventConsumer.read(30, TimeUnit.SECONDS);
    for (Message message : messages) {
      messageId = message.getId();

      messageProcessed = handleMessage(message);
      if (messageProcessed) {
        eventConsumer.acknowledge(messageId);
      }
    }
  }

  private boolean handleMessage(Message message) {
    try {
      featureFlagChangeEventMessageProcessor.processMessage(message);
      return true;
    } catch (Exception ex) {
      // This is not evicted from events framework so that it can be processed
      // by other consumer if the error is a runtime error
      log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
      return false;
    }
  }
}
