package io.harness.ccm.eventframework;

import static io.harness.AuthorizationServiceHeader.CE_NEXT_GEN;
import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CONNECTOR_ENTITY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.ng.core.event.MessageListener;
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

@Slf4j
@Singleton
@OwnedBy(CE)
public class EntityCRUDStreamConsumer implements Runnable {
  private final Consumer redisConsumer;
  private final List<MessageListener> messageListenersList;

  @Inject
  public EntityCRUDStreamConsumer(@Named(ENTITY_CRUD) Consumer redisConsumer,
      @Named(CONNECTOR_ENTITY + ENTITY_CRUD) MessageListener connectorEntityCRUDStreamListener) {
    this.redisConsumer = redisConsumer;
    messageListenersList = new ArrayList<>();
    messageListenersList.add(connectorEntityCRUDStreamListener);
  }

  @Override
  public void run() {
    log.info("Started the consumer for entity crud stream");
    SecurityContextBuilder.setContext(new ServicePrincipal(CE_NEXT_GEN.getServiceId()));
    try {
      while (!Thread.currentThread().isInterrupted()) {
        pollAndProcessMessages();
      }
    } catch (Exception ex) {
      log.error("Entity crud stream consumer unexpectedly stopped", ex);
    }
    SecurityContextBuilder.unsetCompleteContext();
  }

  private void pollAndProcessMessages() throws ConsumerShutdownException {
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
