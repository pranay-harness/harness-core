package io.harness.ng.core.event;

import static io.harness.EntityCRUDEventsConstants.ACCOUNT_ENTITY;
import static io.harness.EntityCRUDEventsConstants.ACTIVITY_ENTITY;
import static io.harness.EntityCRUDEventsConstants.ENTITY_CRUD;
import static io.harness.EntityCRUDEventsConstants.ENTITY_TYPE_METADATA;
import static io.harness.EntityCRUDEventsConstants.ORGANIZATION_ENTITY;
import static io.harness.EntityCRUDEventsConstants.PROJECT_ENTITY;
import static io.harness.EntityCRUDEventsConstants.SETUP_USAGE_ENTITY;

import io.harness.eventsframework.api.AbstractConsumer;
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
  private final AbstractConsumer redisConsumer;
  private final Map<String, ConsumerMessageProcessor> processorMap;

  @Inject
  public EntityCRUDStreamConsumer(@Named(ENTITY_CRUD) AbstractConsumer redisConsumer,
      @Named(ACCOUNT_ENTITY) ConsumerMessageProcessor accountChangeEventMessageProcessor,
      @Named(ORGANIZATION_ENTITY) ConsumerMessageProcessor organizationChangeEventMessageProcessor,
      @Named(PROJECT_ENTITY) ConsumerMessageProcessor projectChangeEventMessageProcessor,
      @Named(SETUP_USAGE_ENTITY) ConsumerMessageProcessor setupUsageChangeEventMessageProcessor,
      @Named(ACTIVITY_ENTITY) ConsumerMessageProcessor entityActivityCrudEventMessageProcessor) {
    this.redisConsumer = redisConsumer;
    processorMap = new HashMap<>();
    processorMap.put(ACCOUNT_ENTITY, accountChangeEventMessageProcessor);
    processorMap.put(ORGANIZATION_ENTITY, organizationChangeEventMessageProcessor);
    processorMap.put(PROJECT_ENTITY, projectChangeEventMessageProcessor);
    processorMap.put(SETUP_USAGE_ENTITY, setupUsageChangeEventMessageProcessor);
    processorMap.put(ACTIVITY_ENTITY, entityActivityCrudEventMessageProcessor);
  }

  @Override
  public void run() {
    log.info("Started the consumer for entity crud stream");
    try {
      while (true) {
        List<Message> messages = redisConsumer.read(2, TimeUnit.SECONDS);
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
      log.error("The consumer for entity crud stream ended", ex);
    }
  }

  private void processMessage(Message message) {
    if (message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.get(ENTITY_TYPE_METADATA) != null) {
        String entityType = metadataMap.get(ENTITY_TYPE_METADATA);
        if (processorMap.get(entityType) != null) {
          processorMap.get(entityType).processMessage(message);
        }
      }
    }
  }
}
