package io.harness.ng.core.entitysetupusage.event;

import static io.harness.ng.eventsframework.EventsFrameworkModule.SETUP_USAGE_DELETE;

import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.entitysetupusage.DeleteSetupUsageDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class EntitySetupUsageDeleteEventsConsumer implements Runnable {
  @Inject EntitySetupUsageService entitySetupUsageService;
  @Inject @Named(SETUP_USAGE_DELETE) AbstractConsumer redisConsumer;
  private static int CONSUMER_SLEEP_TIME = 1000;

  @Override
  public void run() {
    log.info("Started the consumer for deleting the entity setup usage");
    try {
      while (true) {
        Optional<Message> messageOptional = redisConsumer.read();
        if (!messageOptional.isPresent()) {
          sleepConsumer();
          continue;
        }
        String messageId = messageOptional.get().getId();
        DeleteSetupUsageDTO deleteRequestDTO = getEntitySetupUsageDeleteDTO(messageOptional.get());
        entitySetupUsageService.delete(deleteRequestDTO.getAccountIdentifier(), deleteRequestDTO.getReferredEntityFQN(),
            deleteRequestDTO.getReferredByEntityFQN());
        log.info("Received setup usage delete event for referredEntity {}, referredBy {}, with messageId {}",
            deleteRequestDTO.getReferredEntityFQN(), deleteRequestDTO.getReferredByEntityFQN(), messageId);
        redisConsumer.acknowledge(messageId);
        sleepConsumer();
      }
    } catch (Exception ex) {
      log.info("The consumer for deleting the entity setup usage ended", ex);
    }
  }

  private void sleepConsumer() {
    try {
      Thread.sleep(CONSUMER_SLEEP_TIME);
    } catch (InterruptedException ex) {
      log.error("The thread processing the setup usage deletion got interrupted");
    }
  }

  private DeleteSetupUsageDTO getEntitySetupUsageDeleteDTO(Message entityDeleteMessage) {
    DeleteSetupUsageDTO deleteRequestDTO = null;
    try {
      deleteRequestDTO = DeleteSetupUsageDTO.parseFrom(entityDeleteMessage.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking DeleteSetupUsageDTO for key {}", entityDeleteMessage.getId(), e);
    }
    return deleteRequestDTO;
  }
}