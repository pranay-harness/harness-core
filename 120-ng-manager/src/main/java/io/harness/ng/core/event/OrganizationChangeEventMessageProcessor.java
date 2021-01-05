package io.harness.ng.core.event;

import static io.harness.exception.WingsException.USER;

import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class OrganizationChangeEventMessageProcessor implements MessageProcessor {
  private final HarnessSMManager harnessSMManager;

  @Override
  public void processMessage(Message message) {
    if (!validateMessage(message)) {
      if (message != null) {
        throw new InvalidRequestException(String.format(
            "Invalid message received by Organization Change Event Processor with message id %s", message.getId()));
      } else {
        throw new InvalidRequestException("Null message received by Organization Change Event Processor");
      }
    }

    OrganizationEntityChangeDTO organizationEntityChangeDTO;
    try {
      organizationEntityChangeDTO = OrganizationEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking OrganizationEntityChangeDTO for key %s", message.getId()), e);
    }

    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap.get(EventsFrameworkMetadataConstants.ACTION) != null) {
      switch (metadataMap.get(EventsFrameworkMetadataConstants.ACTION)) {
        case EventsFrameworkMetadataConstants.CREATE_ACTION:
          processCreateAction(organizationEntityChangeDTO);
          return;
        case EventsFrameworkMetadataConstants.DELETE_ACTION:
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
        && EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY.equals(
            message.getMessage().getMetadataMap().get(EventsFrameworkMetadataConstants.ENTITY_TYPE));
  }
}
