package io.harness.ng.core.event;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkConstants.FEATURE_FLAG_STREAM;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CONNECTOR_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.consumer.Message;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class FeatureFlagStreamConsumer implements Runnable {
  private final Consumer eventConsumer;
  private final List<MessageListener> messageListenersList;

  @Inject
  public FeatureFlagStreamConsumer(@Named(FEATURE_FLAG_STREAM) Consumer eventConsumer,
      @Named(ORGANIZATION_ENTITY + FEATURE_FLAG_STREAM) MessageListener organizationFeatureFlagStreamListener,
      @Named(CONNECTOR_ENTITY + FEATURE_FLAG_STREAM) MessageListener connectorFeatureFlagStreamListener,
      @Named("access_control_migration" + FEATURE_FLAG_STREAM) MessageListener accessControlMigrationHandler) {
    this.eventConsumer = eventConsumer;
    messageListenersList = new ArrayList<>();
    messageListenersList.add(organizationFeatureFlagStreamListener);
    messageListenersList.add(connectorFeatureFlagStreamListener);
    messageListenersList.add(accessControlMigrationHandler);
  }

  @Override
  public void run() {
    log.info("Started the consumer for feature flag stream");
    SecurityContextBuilder.setContext(new ServicePrincipal(NG_MANAGER.getServiceId()));
    try {
      while (!Thread.currentThread().isInterrupted()) {
        pollAndProcessMessages();
      }
    } catch (Exception ex) {
      log.error("Feature flag stream consumer unexpectedly stopped", ex);
    }
    SecurityContextBuilder.unsetCompleteContext();
  }

  private void pollAndProcessMessages() {
    String messageId;
    boolean messageProcessed;
    List<Message> messages;
    messages = eventConsumer.read(Duration.ofSeconds(30));
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
      messageListenersList.forEach(messageListener -> {
        if (!messageListener.handleMessage(message)) {
          success.set(false);
        }
      });
      return success.get();
    } catch (Exception ex) {
      // This is not evicted from events framework so that it can be processed
      // by other consumer if the error is a runtime error
      log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
      return false;
    }
  }
}
