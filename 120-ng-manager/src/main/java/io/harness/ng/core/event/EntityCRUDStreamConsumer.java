package io.harness.ng.core.event;

import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.consumer.Message;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class EntityCRUDStreamConsumer implements Runnable {
  private final Consumer redisConsumer;
  private final Map<String, MessageProcessor> processorMap;

  @Inject
  public EntityCRUDStreamConsumer(@Named(EventsFrameworkConstants.ENTITY_CRUD) Consumer redisConsumer,
      @Named(EventsFrameworkMetadataConstants.ACCOUNT_ENTITY) MessageProcessor accountChangeEventMessageProcessor,
      @Named(EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY)
      MessageProcessor organizationChangeEventMessageProcessor,
      @Named(EventsFrameworkMetadataConstants.PROJECT_ENTITY) MessageProcessor projectChangeEventMessageProcessor,
      @Named(
          EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY) MessageProcessor setupUsageChangeEventMessageProcessor,
      @Named(
          EventsFrameworkMetadataConstants.ACTIVITY_ENTITY) MessageProcessor entityActivityCrudEventMessageProcessor) {
    this.redisConsumer = redisConsumer;
    processorMap = new HashMap<>();
    processorMap.put(EventsFrameworkMetadataConstants.ACCOUNT_ENTITY, accountChangeEventMessageProcessor);
    processorMap.put(EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY, organizationChangeEventMessageProcessor);
    processorMap.put(EventsFrameworkMetadataConstants.PROJECT_ENTITY, projectChangeEventMessageProcessor);
    processorMap.put(EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY, setupUsageChangeEventMessageProcessor);
    processorMap.put(EventsFrameworkMetadataConstants.ACTIVITY_ENTITY, entityActivityCrudEventMessageProcessor);
  }

  @Override
  public void run() {
    log.info("Started the consumer for entity crud stream");
    try {
      while (!Thread.currentThread().isInterrupted()) {
        List<Message> messages = redisConsumer.read(10, TimeUnit.SECONDS);
        for (Message message : messages) {
          String messageId = message.getId();
          try {
            processMessage(message);
          } catch (Exception ex) {
            log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
            continue;
          }
          redisConsumer.acknowledge(messageId);
        }
      }
    } catch (Exception ex) {
      log.error("Entity crud stream consumer unexpectedly stopped", ex);
    }
  }

  private void processMessage(Message message) {
    if (message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.get(EventsFrameworkMetadataConstants.ENTITY_TYPE) != null) {
        String entityType = metadataMap.get(EventsFrameworkMetadataConstants.ENTITY_TYPE);
        if (processorMap.get(entityType) != null) {
          processorMap.get(entityType).processMessage(message);
        }
      }
    }
  }
}
