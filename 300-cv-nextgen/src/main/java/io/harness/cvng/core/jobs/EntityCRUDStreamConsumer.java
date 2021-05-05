package io.harness.cvng.core.jobs;

import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.consumer.Message;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class EntityCRUDStreamConsumer implements Runnable {
  private static final int MAX_WAIT_TIME_SEC = 2;
  private final Consumer consumer;
  private final Map<String, ConsumerMessageProcessor> processorMap;

  @Inject
  public EntityCRUDStreamConsumer(@Named(EventsFrameworkConstants.ENTITY_CRUD) Consumer abstractConsumer,
      @Named(
          EventsFrameworkMetadataConstants.PROJECT_ENTITY) ConsumerMessageProcessor projectChangeEventMessageProcessor,
      @Named(EventsFrameworkMetadataConstants.CONNECTOR_ENTITY)
      ConsumerMessageProcessor connectorChangeEventMessageProcessor,
      @Named(EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY)
      ConsumerMessageProcessor organizationChangeEventMessageProcessor,
      @Named(EventsFrameworkMetadataConstants.ACCOUNT_ENTITY)
      ConsumerMessageProcessor accountChangeEventMessageProcessor) {
    this.consumer = abstractConsumer;
    processorMap = new HashMap<>();
    processorMap.put(EventsFrameworkMetadataConstants.PROJECT_ENTITY, projectChangeEventMessageProcessor);
    processorMap.put(EventsFrameworkMetadataConstants.CONNECTOR_ENTITY, connectorChangeEventMessageProcessor);
    processorMap.put(EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY, organizationChangeEventMessageProcessor);
    processorMap.put(EventsFrameworkMetadataConstants.ACCOUNT_ENTITY, accountChangeEventMessageProcessor);
  }

  @Override
  public void run() {
    log.info("Started the consumer for entity crud stream");
    try {
      while (true) {
        pollAndProcessMessages();
      }
    } catch (Exception ex) {
      log.error("Entity crud stream consumer unexpectedly stopped", ex);
    }
  }

  private void pollAndProcessMessages() {
    List<Message> messages;
    String messageId;
    boolean messageProcessed;
    messages = consumer.read(Duration.ofSeconds(MAX_WAIT_TIME_SEC));
    for (Message message : messages) {
      messageId = message.getId();
      messageProcessed = handleMessage(message);
      if (messageProcessed) {
        consumer.acknowledge(messageId);
      }
    }
  }

  private boolean handleMessage(Message message) {
    try {
      processMessage(message);
      return true;
    } catch (Exception ex) {
      // This is not evicted from events framework so that it can be processed
      // by other consumer if the error is a runtime error
      log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
      return false;
    }
  }

  private void processMessage(Message message) {
    if (message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.containsKey(EventsFrameworkMetadataConstants.ENTITY_TYPE)) {
        String entityType = metadataMap.get(EventsFrameworkMetadataConstants.ENTITY_TYPE);
        if (processorMap.containsKey(entityType)) {
          try {
            processorMap.get(entityType).processMessage(message);
          } catch (Exception ex) {
            log.error(String.format("Error occurred in processing message of entityType %s and id %s", entityType,
                          message.getId()),
                ex);
          }
        }
      }
    }
  }
}
