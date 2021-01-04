package io.harness.cvng.core.jobs;

import io.harness.eventsframework.EventsFrameworkConstants;
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
  private static final int MAX_WAIT_TIME_SEC = 2;
  private final AbstractConsumer consumer;
  private final Map<String, ConsumerMessageProcessor> processorMap;

  @Inject
  public EntityCRUDStreamConsumer(@Named(EventsFrameworkConstants.ENTITY_CRUD) AbstractConsumer abstractConsumer,
      @Named(EventsFrameworkConstants.PROJECT_ENTITY) ConsumerMessageProcessor projectChangeEventMessageProcessor,
      @Named(EventsFrameworkConstants.CONNECTOR_ENTITY) ConsumerMessageProcessor connectorChangeEventMessageProcessor) {
    this.consumer = abstractConsumer;
    processorMap = new HashMap<>();
    processorMap.put(EventsFrameworkConstants.PROJECT_ENTITY, projectChangeEventMessageProcessor);
    processorMap.put(EventsFrameworkConstants.CONNECTOR_ENTITY, connectorChangeEventMessageProcessor);
  }

  @Override
  public void run() {
    log.info("Started the consumer for entity crud stream");
    try {
      while (true) {
        List<Message> messages = consumer.read(MAX_WAIT_TIME_SEC, TimeUnit.SECONDS);
        for (Message message : messages) {
          String messageId = message.getId();
          processMessage(message);
          consumer.acknowledge(messageId);
        }
      }
    } catch (Exception ex) {
      log.info("The consumer for entity crud stream ended", ex);
    }
  }

  private void processMessage(Message message) {
    if (message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.containsKey(EventsFrameworkConstants.ENTITY_TYPE_METADATA)) {
        String entityType = metadataMap.get(EventsFrameworkConstants.ENTITY_TYPE_METADATA);
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
