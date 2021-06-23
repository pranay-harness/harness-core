package io.harness.pms.event.featureflag;

import static io.harness.AuthorizationServiceHeader.PIPELINE_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.eventsframework.EventsFrameworkConstants.FEATURE_FLAG_STREAM;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.PIPELINE_ENTITY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.ng.core.event.MessageListener;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;
import io.harness.threading.Morpheus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(PIPELINE)
public class PipelineServiceFeatureFlagConsumer implements Runnable {
  private static final int WAIT_TIME_IN_SECONDS = 30;

  Consumer eventConsumer;
  MessageListener pipelineFeatureFlagListener;

  @Inject
  public PipelineServiceFeatureFlagConsumer(@Named(FEATURE_FLAG_STREAM) Consumer redisConsumer,
      @Named(PIPELINE_ENTITY + FEATURE_FLAG_STREAM) MessageListener pipelineFeatureFlagListener) {
    this.eventConsumer = redisConsumer;
    this.pipelineFeatureFlagListener = pipelineFeatureFlagListener;
  }

  @Override
  public void run() {
    log.info("Started the consumer for feature flag stream");
    SecurityContextBuilder.setContext(new ServicePrincipal(PIPELINE_SERVICE.getServiceId()));
    try {
      while (!Thread.currentThread().isInterrupted()) {
        readEventsFrameworkMessages();
        Morpheus.sleep(Duration.ofSeconds(10));
      }
    } catch (Exception ex) {
      log.error("Feature flag stream consumer unexpectedly stopped", ex);
    }
    SecurityContextBuilder.unsetCompleteContext();
  }

  private void readEventsFrameworkMessages() throws InterruptedException {
    try {
      pollAndProcessMessages();
    } catch (EventsFrameworkDownException e) {
      log.error("Events framework is down for Feature flag consumer. Retrying again...", e);
      TimeUnit.SECONDS.sleep(WAIT_TIME_IN_SECONDS);
    }
  }

  private void pollAndProcessMessages() {
    String messageId;
    boolean messageProcessed;
    List<Message> messages;
    messages = eventConsumer.read(Duration.ofSeconds(WAIT_TIME_IN_SECONDS));
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
      AtomicBoolean success = new AtomicBoolean(true);
      if (!pipelineFeatureFlagListener.handleMessage(message)) {
        success.set(false);
      }
      return success.get();
    } catch (Exception ex) {
      // This is not evicted from events framework so that it can be processed
      // by other consumer if the error is a runtime error
      log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
      return false;
    }
  }
}
