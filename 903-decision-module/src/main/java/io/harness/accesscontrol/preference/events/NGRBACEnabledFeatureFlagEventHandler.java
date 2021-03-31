package io.harness.accesscontrol.preference.events;

import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.accesscontrol.preference.services.AccessControlPreferenceService;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@OwnedBy(HarnessTeam.PL)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class NGRBACEnabledFeatureFlagEventHandler implements EventHandler {
  private final AccessControlPreferenceService accessControlPreferenceService;

  @SneakyThrows
  @Override
  public boolean handle(Message message) {
    FeatureFlagChangeDTO featureFlagChangeDTO = FeatureFlagChangeDTO.parseFrom(message.getMessage().getData());
    return accessControlPreferenceService.upsertAccessControlEnabled(
        featureFlagChangeDTO.getAccountId(), featureFlagChangeDTO.getEnable());
  }
}
