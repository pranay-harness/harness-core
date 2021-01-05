package io.harness.ng.core.event;

import static io.harness.beans.FeatureName.NEXT_GEN_ENABLED;
import static io.harness.exception.WingsException.USER;

import io.harness.beans.FeatureName;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class FeatureFlagChangeEventMessageProcessor implements MessageProcessor {
  private final HarnessSMManager harnessSMManager;
  private final DefaultOrganizationManager defaultOrganizationManager;

  @Override
  public void processMessage(Message message) {
    if (message == null) {
      throw new InvalidRequestException("Null message received by Feature Flag Change Event Processor");
    } else if (!message.hasMessage()) {
      throw new InvalidRequestException(String.format(
          "Invalid message received by Feature Flag Change Event Processor with message id %s", message.getId()));
    }
    FeatureFlagChangeDTO featureFlagChangeDTO;
    try {
      featureFlagChangeDTO = FeatureFlagChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking FeatureFlagChangeDTO for key %s", message.getId()), e);
    }

    if (NEXT_GEN_ENABLED.equals(FeatureName.valueOf(featureFlagChangeDTO.getFeatureName()))) {
      if (featureFlagChangeDTO.getEnable()) {
        processNGEnableAction(featureFlagChangeDTO.getAccountId());
      }
    }
  }

  private void processNGEnableAction(String accountId) {
    try {
      harnessSMManager.createHarnessSecretManager(accountId, null, null);
    } catch (Exception ex) {
      log.error(
          String.format("Harness Secret Manager could not be created for accountIdentifier %s", accountId), ex, USER);
    }

    try {
      defaultOrganizationManager.createDefaultOrganization(accountId);
    } catch (DuplicateKeyException ex) {
      log.info(String.format("Default Organization for accountIdentifier %s already exists", accountId), ex, USER);
    }
  }
}
