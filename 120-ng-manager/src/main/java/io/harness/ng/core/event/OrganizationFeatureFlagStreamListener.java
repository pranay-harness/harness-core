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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@Slf4j
@Singleton
public class OrganizationFeatureFlagStreamListener implements MessageListener {
  private final DefaultOrganizationManager defaultOrganizationManager;

  @Inject
  public OrganizationFeatureFlagStreamListener(DefaultOrganizationManager defaultOrganizationManager) {
    this.defaultOrganizationManager = defaultOrganizationManager;
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
      defaultOrganizationManager.createDefaultOrganization(accountId);
    } catch (DuplicateKeyException ex) {
      log.info(String.format("Default Organization for accountIdentifier %s already exists", accountId), ex, USER);
    }
    return true;
  }
}
