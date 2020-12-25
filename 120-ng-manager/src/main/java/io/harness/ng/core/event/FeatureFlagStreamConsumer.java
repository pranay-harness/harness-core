package io.harness.ng.core.event;

import static io.harness.ff.FeatureFlagServiceImpl.FEATURE_FLAG_STREAM;

import io.harness.eventsframework.api.AbstractConsumer;
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
  private final AbstractConsumer redisConsumer;
  private final ConsumerMessageProcessor featureFlagChangeEventMessageProcessor;

  @Inject
  public FeatureFlagStreamConsumer(@Named(FEATURE_FLAG_STREAM) AbstractConsumer redisConsumer,
      @Named(FEATURE_FLAG_STREAM) ConsumerMessageProcessor featureFlagChangeEventMessageProcessor) {
    this.redisConsumer = redisConsumer;
    this.featureFlagChangeEventMessageProcessor = featureFlagChangeEventMessageProcessor;
  }

  @Override
  public void run() {
    log.info("Started the consumer for feature flag stream");
    try {
      while (true) {
        List<Message> messages = redisConsumer.read(10, TimeUnit.SECONDS);
        for (Message message : messages) {
          String messageId = message.getId();
          try {
            featureFlagChangeEventMessageProcessor.processMessage(message);
          } catch (Exception ex) {
            log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
            continue;
          }
          redisConsumer.acknowledge(messageId);
        }
      }
    } catch (Exception ex) {
      log.info("The consumer for feature flag stream ended", ex);
    }
  }
}
