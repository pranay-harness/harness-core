package io.harness.ng.core.event;

import static io.harness.beans.FeatureName.NEXT_GEN_ENABLED;
import static io.harness.exception.WingsException.USER;

import io.harness.beans.FeatureName;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ConnectorFeatureFlagStreamListener implements MessageListener {
  private final HarnessSMManager harnessSMManager;

  @Inject
  public ConnectorFeatureFlagStreamListener(HarnessSMManager harnessSMManager) {
    this.harnessSMManager = harnessSMManager;
  }

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      FeatureFlagChangeDTO featureFlagChangeDTO;
      try {
        featureFlagChangeDTO = FeatureFlagChangeDTO.parseFrom(message.getMessage().getData());
      } catch (InvalidProtocolBufferException e) {
        throw new InvalidRequestException(
            String.format("Exception in unpacking FeatureFlagChangeDTO for key %s", message.getId()), e);
      }
      if (NEXT_GEN_ENABLED.equals(FeatureName.valueOf(featureFlagChangeDTO.getFeatureName()))) {
        if (featureFlagChangeDTO.getEnable()) {
          return processNGEnableAction(featureFlagChangeDTO.getAccountId());
        }
      }
    }
    return true;
  }

  private boolean processNGEnableAction(String accountId) {
    try {
      harnessSMManager.createHarnessSecretManager(accountId, null, null);
    } catch (DuplicateFieldException ex) {
      log.error(String.format("Harness Secret Manager for accountIdentifier %s already exists", accountId), ex, USER);
    }
    return true;
  }
}
