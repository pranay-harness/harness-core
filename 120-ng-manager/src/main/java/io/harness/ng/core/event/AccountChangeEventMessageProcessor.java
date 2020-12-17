package io.harness.ng.core.event;

import static io.harness.EntityCRUDEventsConstants.ACCOUNT_ENTITY;
import static io.harness.EntityCRUDEventsConstants.ACTION_METADATA;
import static io.harness.EntityCRUDEventsConstants.CREATE_ACTION;
import static io.harness.EntityCRUDEventsConstants.DELETE_ACTION;
import static io.harness.EntityCRUDEventsConstants.ENTITY_TYPE_METADATA;
import static io.harness.exception.WingsException.USER;

import io.harness.eventsframework.account.AccountEntityChangeDTO;
import io.harness.eventsframework.consumer.Message;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class AccountChangeEventMessageProcessor implements ConsumerMessageProcessor {
  private final HarnessSMManager harnessSMManager;
  private final DefaultOrganizationManager defaultOrganizationManager;

  @Inject
  public AccountChangeEventMessageProcessor(
      HarnessSMManager harnessSMManager, DefaultOrganizationManager defaultOrganizationManager) {
    this.harnessSMManager = harnessSMManager;
    this.defaultOrganizationManager = defaultOrganizationManager;
  }

  @Override
  public void processMessage(Message message) {
    if (!validateMessage(message)) {
      log.error("Invalid message received by Account Change Event Processor");
      return;
    }

    AccountEntityChangeDTO accountEntityChangeDTO;
    try {
      accountEntityChangeDTO = AccountEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking AccountEntityChangeDTO for key {}", message.getId(), e);
      return;
    }

    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap.containsKey(ACTION_METADATA)) {
      switch (metadataMap.get(ACTION_METADATA)) {
        case CREATE_ACTION:
          processCreateAction(accountEntityChangeDTO);
          return;
        case DELETE_ACTION:
          processDeleteAction(accountEntityChangeDTO);
          return;
        default:
      }
    }
  }

  private void processCreateAction(AccountEntityChangeDTO accountEntityChangeDTO) {
    try {
      harnessSMManager.createHarnessSecretManager(accountEntityChangeDTO.getAccountId(), null, null);
    } catch (Exception ex) {
      log.error(String.format("Harness Secret Manager could not be created for accountIdentifier %s",
                    accountEntityChangeDTO.getAccountId()),
          ex, USER);
    }

    try {
      defaultOrganizationManager.createDefaultOrganization(accountEntityChangeDTO.getAccountId());
    } catch (Exception ex) {
      log.error(String.format("Failed to create Default Organization for accountIdentifier %s",
                    accountEntityChangeDTO.getAccountId()),
          ex, USER);
    }
  }

  private void processDeleteAction(AccountEntityChangeDTO accountEntityChangeDTO) {
    try {
      if (!harnessSMManager.deleteHarnessSecretManager(accountEntityChangeDTO.getAccountId(), null, null)) {
        log.error(String.format("Harness Secret Manager could not be deleted for accountIdentifier %s",
                      accountEntityChangeDTO.getAccountId()),
            USER);
      }
    } catch (Exception ex) {
      log.error(String.format("Harness Secret Manager could not be deleted for accountIdentifier %s",
                    accountEntityChangeDTO.getAccountId()),
          ex, USER);
    }

    try {
      if (!defaultOrganizationManager.deleteDefaultOrganization(accountEntityChangeDTO.getAccountId())) {
        log.error(String.format("Default Organization could not be deleted for accountIdentifier %s",
                      accountEntityChangeDTO.getAccountId()),
            USER);
      }
    } catch (Exception ex) {
      log.error(String.format("Default Organization could not be deleted for accountIdentifier %s",
                    accountEntityChangeDTO.getAccountId()),
          ex, USER);
    }
  }

  private boolean validateMessage(Message message) {
    return message != null && message.hasMessage() && message.getMessage().getMetadataMap() != null
        && message.getMessage().getMetadataMap().containsKey(ENTITY_TYPE_METADATA)
        && ACCOUNT_ENTITY.equals(message.getMessage().getMetadataMap().get(ENTITY_TYPE_METADATA));
  }
}
