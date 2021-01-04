package io.harness.ng.core.event;

import static io.harness.exception.WingsException.USER;

import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.account.AccountEntityChangeDTO;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class AccountChangeEventMessageProcessor implements ConsumerMessageProcessor {
  private final HarnessSMManager harnessSMManager;
  private final DefaultOrganizationManager defaultOrganizationManager;

  @Override
  public void processMessage(Message message) {
    if (!validateMessage(message)) {
      if (message != null) {
        throw new InvalidRequestException(String.format(
            "Invalid message received by Account Change Event Processor with message id %s", message.getId()));
      } else {
        throw new InvalidRequestException("Null message received by Account Change Event Processor");
      }
    }

    AccountEntityChangeDTO accountEntityChangeDTO;
    try {
      accountEntityChangeDTO = AccountEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking AccountEntityChangeDTO for key %s", message.getId()), e);
    }

    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap.get(EventsFrameworkConstants.ACTION_METADATA) != null) {
      switch (metadataMap.get(EventsFrameworkConstants.ACTION_METADATA)) {
        case EventsFrameworkConstants.CREATE_ACTION:
          processCreateAction(accountEntityChangeDTO);
          return;
        case EventsFrameworkConstants.DELETE_ACTION:
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
    } catch (DuplicateKeyException ex) {
      log.error(String.format("Default Organization for accountIdentifier %s already exists",
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
    defaultOrganizationManager.deleteDefaultOrganization(accountEntityChangeDTO.getAccountId());
  }

  private boolean validateMessage(Message message) {
    return message != null && message.hasMessage() && message.getMessage().getMetadataMap() != null
        && EventsFrameworkConstants.ACCOUNT_ENTITY.equals(
            message.getMessage().getMetadataMap().get(EventsFrameworkConstants.ENTITY_TYPE_METADATA));
  }
}
