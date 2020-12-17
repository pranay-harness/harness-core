package io.harness.ng.core.event;

import static io.harness.EntityCRUDEventsConstants.ACTION_METADATA;
import static io.harness.EntityCRUDEventsConstants.CREATE_ACTION;
import static io.harness.EntityCRUDEventsConstants.DELETE_ACTION;
import static io.harness.EntityCRUDEventsConstants.ENTITY_TYPE_METADATA;
import static io.harness.EntityCRUDEventsConstants.ORGANIZATION_ENTITY;
import static io.harness.exception.WingsException.USER;

import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.organization.OrganizationEntityChangeDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class OrganizationChangeEventMessageProcessor implements ConsumerMessageProcessor {
  private final HarnessSMManager harnessSMManager;

  @Inject
  public OrganizationChangeEventMessageProcessor(HarnessSMManager harnessSMManager) {
    this.harnessSMManager = harnessSMManager;
  }

  @Override
  public void processMessage(Message message) {
    if (!validateMessage(message)) {
      log.error("Invalid message received by Organization Change Event Processor");
      return;
    }

    OrganizationEntityChangeDTO organizationEntityChangeDTO;
    try {
      organizationEntityChangeDTO = OrganizationEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking OrganizationEntityChangeDTO for key {}", message.getId(), e);
      return;
    }

    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap.containsKey(ACTION_METADATA)) {
      switch (metadataMap.get(ACTION_METADATA)) {
        case CREATE_ACTION:
          processCreateAction(organizationEntityChangeDTO);
          return;
        case DELETE_ACTION:
          processDeleteAction(organizationEntityChangeDTO);
          return;
        default:
      }
    }
  }

  private void processCreateAction(OrganizationEntityChangeDTO organizationEntityChangeDTO) {
    try {
      harnessSMManager.createHarnessSecretManager(
          organizationEntityChangeDTO.getAccountIdentifier(), organizationEntityChangeDTO.getIdentifier(), null);
    } catch (Exception ex) {
      log.error(
          String.format("Harness Secret Manager could not be created for accountIdentifier %s and orgIdentifier %s",
              organizationEntityChangeDTO.getAccountIdentifier(), organizationEntityChangeDTO.getIdentifier()),
          ex, USER);
    }
  }

  private void processDeleteAction(OrganizationEntityChangeDTO organizationEntityChangeDTO) {
    try {
      if (!harnessSMManager.deleteHarnessSecretManager(
              organizationEntityChangeDTO.getAccountIdentifier(), organizationEntityChangeDTO.getIdentifier(), null)) {
        log.error(
            String.format("Harness Secret Manager could not be deleted for accountIdentifier %s and orgIdentifier %s",
                organizationEntityChangeDTO.getAccountIdentifier(), organizationEntityChangeDTO.getIdentifier()),
            USER);
      }
    } catch (Exception ex) {
      log.error(
          String.format("Harness Secret Manager could not be deleted for accountIdentifier %s and orgIdentifier %s",
              organizationEntityChangeDTO.getAccountIdentifier(), organizationEntityChangeDTO.getIdentifier()),
          ex, USER);
    }
  }

  private boolean validateMessage(Message message) {
    return message != null && message.hasMessage() && message.getMessage().getMetadataMap() != null
        && message.getMessage().getMetadataMap().containsKey(ENTITY_TYPE_METADATA)
        && ORGANIZATION_ENTITY.equals(message.getMessage().getMetadataMap().get(ENTITY_TYPE_METADATA));
  }
}
