package io.harness.ng.core.entitysetupusage.event;

import static io.harness.ng.eventsframework.EventsFrameworkModule.SETUP_USAGE_CREATE;

import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.mapper.EntityEventDTOToRestDTOMapper;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class EntitySetupUsageCreateEventsConsumer implements Runnable {
  EntitySetupUsageService entitySetupUsageService;
  EntityEventDTOToRestDTOMapper entityEventDTOToRestDTOMapper;
  AbstractConsumer redisConsumer;
  private static int CONSUMER_SLEEP_TIME = 1000;

  @Inject
  public EntitySetupUsageCreateEventsConsumer(EntitySetupUsageService entitySetupUsageService,
      EntityEventDTOToRestDTOMapper entityEventDTOToRestDTOMapper,
      @Named(SETUP_USAGE_CREATE) AbstractConsumer redisConsumer) {
    this.entitySetupUsageService = entitySetupUsageService;
    this.entityEventDTOToRestDTOMapper = entityEventDTOToRestDTOMapper;
    this.redisConsumer = redisConsumer;
  }

  @Override
  public void run() {
    log.info("Started the consumer for creating the entity setup usage");
    try {
      while (true) {
        Optional<Message> messageOptional = redisConsumer.read();
        if (!messageOptional.isPresent()) {
          sleepConsumer();
          continue;
        }
        String messageId = messageOptional.get().getId();
        EntitySetupUsageCreateDTO setupUsageCreateDTO = getEntitySetupUsageCreateDTO(messageOptional.get());
        EntitySetupUsageDTO entitySetupUsageDTO = entityEventDTOToRestDTOMapper.toRestDTO(setupUsageCreateDTO);
        entitySetupUsageService.save(entitySetupUsageDTO);
        log.info("Received setup usage creation event for {} with messageId {}",
            entitySetupUsageDTO.getReferredEntity().getEntityRef().getFullyQualifiedName(), messageId);
        redisConsumer.acknowledge(messageId);
        sleepConsumer();
      }
    } catch (Exception ex) {
      log.info("The consumer for creating the entity setup usage ended", ex);
    }
  }

  private void sleepConsumer() {
    try {
      Thread.sleep(CONSUMER_SLEEP_TIME);
    } catch (InterruptedException ex) {
      log.error("The thread processing the setup usage creation got interrupted");
    }
  }

  private EntitySetupUsageCreateDTO getEntitySetupUsageCreateDTO(Message entitySetupUsageMessage) {
    EntitySetupUsageCreateDTO entitySetupUsageCreateDTO = null;
    try {
      entitySetupUsageCreateDTO = EntitySetupUsageCreateDTO.parseFrom(entitySetupUsageMessage.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking EntitySetupUsageCreateDTO   for key {}", entitySetupUsageMessage.getId(), e);
    }
    return entitySetupUsageCreateDTO;
  }
}