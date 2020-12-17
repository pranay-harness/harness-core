package io.harness.ng.core.event;

import static io.harness.EntityCRUDEventsConstants.ACCOUNT_ENTITY;
import static io.harness.EntityCRUDEventsConstants.ENTITY_CRUD;
import static io.harness.EntityCRUDEventsConstants.ENTITY_TYPE_METADATA;
import static io.harness.EntityCRUDEventsConstants.ORGANIZATION_ENTITY;
import static io.harness.EntityCRUDEventsConstants.PROJECT_ENTITY;

import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.consumer.Message;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class EntityCRUDStreamConsumer implements Runnable {
  private final AbstractConsumer redisConsumer;
  private static final int CONSUMER_SLEEP_TIME = 1000;
  private final Map<String, ConsumerMessageProcessor> processorMap;

  @Inject
  public EntityCRUDStreamConsumer(@Named(ENTITY_CRUD) AbstractConsumer redisConsumer,
      @Named(ACCOUNT_ENTITY) ConsumerMessageProcessor accountChangeEventMessageProcessor,
      @Named(ORGANIZATION_ENTITY) ConsumerMessageProcessor organizationChangeEventMessageProcessor,
      @Named(PROJECT_ENTITY) ConsumerMessageProcessor projectChangeEventMessageProcessor) {
    this.redisConsumer = redisConsumer;
    processorMap = new HashMap<>();
    processorMap.put(ACCOUNT_ENTITY, accountChangeEventMessageProcessor);
    processorMap.put(ORGANIZATION_ENTITY, organizationChangeEventMessageProcessor);
    processorMap.put(PROJECT_ENTITY, projectChangeEventMessageProcessor);
  }

  @Override
  public void run() {
    log.info("Started the consumer for entity crud stream");
    try {
      while (true) {
        Optional<Message> messageOptional = redisConsumer.read();
        if (!messageOptional.isPresent()) {
          sleepConsumer();
          continue;
        }
        String messageId = messageOptional.get().getId();
        processMessage(messageOptional.get());
        redisConsumer.acknowledge(messageId);
        sleepConsumer();
      }
    } catch (Exception ex) {
      log.info("The consumer for entity crud stream ended", ex);
    }
  }

  private void processMessage(Message message) {
    if (message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.containsKey(ENTITY_TYPE_METADATA)) {
        String entityType = metadataMap.get(ENTITY_TYPE_METADATA);
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

  private void sleepConsumer() {
    try {
      Thread.sleep(CONSUMER_SLEEP_TIME);
    } catch (InterruptedException ex) {
      log.error("The thread processing the entity crud stream got interrupted");
    }
  }
}
