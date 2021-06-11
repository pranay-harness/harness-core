package io.harness.ng.core.event;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.FeatureName.NEXT_GEN_ENABLED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.resourcegroup.migration.DefaultResourceGroupCreationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class OrganizationFeatureFlagStreamListener implements MessageListener {
  private final DefaultOrganizationManager defaultOrganizationManager;
  private final DefaultResourceGroupCreationService defaultResourceGroupCreationService;

  @Inject
  public OrganizationFeatureFlagStreamListener(DefaultOrganizationManager defaultOrganizationManager,
      DefaultResourceGroupCreationService defaultResourceGroupCreationService) {
    this.defaultOrganizationManager = defaultOrganizationManager;
    this.defaultResourceGroupCreationService = defaultResourceGroupCreationService;
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
    defaultOrganizationManager.createDefaultOrganization(accountId);
    defaultResourceGroupCreationService.createDefaultResourceGroup(accountId, null, null);
    return true;
  }
}
