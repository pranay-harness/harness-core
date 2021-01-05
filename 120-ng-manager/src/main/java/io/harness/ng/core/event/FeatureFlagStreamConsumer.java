package io.harness.ng.core.event;

import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
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
        List<Message> messages = eventConsumer.read(30, TimeUnit.SECONDS);
        for (Message message : messages) {
          String messageId = message.getId();
          try {
            featureFlagChangeEventMessageProcessor.processMessage(message);
          } catch (Exception ex) {
            log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
            continue;
          }
          eventConsumer.acknowledge(messageId);
        }
      }
    } catch (Exception ex) {
      log.error("Feature flag stream consumer unexpectedly stopped", ex);
    }
  }
}
