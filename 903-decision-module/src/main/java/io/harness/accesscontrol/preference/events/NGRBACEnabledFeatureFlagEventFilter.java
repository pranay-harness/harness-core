package io.harness.accesscontrol.preference.events;

import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class NGRBACEnabledFeatureFlagEventFilter implements EventFilter {
  @Override
  public boolean filter(Message message) {
    FeatureFlagChangeDTO featureFlagChangeDTO = null;
    try {
      featureFlagChangeDTO = FeatureFlagChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking ResourceGroupEntityChangeDTO for key {}", message.getId(), e);
    }

    return Optional.ofNullable(featureFlagChangeDTO)
        .map(FeatureFlagChangeDTO::getFeatureName)
        .filter(featureName -> FeatureName.NG_RBAC_ENABLED.name().equals(featureName))
        .isPresent();
  }
}
