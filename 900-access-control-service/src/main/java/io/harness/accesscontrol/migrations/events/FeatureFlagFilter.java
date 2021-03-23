package io.harness.accesscontrol.migrations.events;

import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeatureFlagFilter implements EventFilter {
  private final Set<String> relevantFeatures = Sets.newHashSet(ImmutableList.of("NG_RBAC_ENABLED"));

  @Override
  public boolean filter(Message message) {
    FeatureFlagChangeDTO featureFlagChangeDTO = null;
    try {
      featureFlagChangeDTO = FeatureFlagChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.info("Exception in unpacking ResourceGroupEntityChangeDTO for key {}", message.getId(), e);
    }

    if (Optional.ofNullable(featureFlagChangeDTO).isPresent()) {
      return featureFlagChangeDTO.getEnable() && relevantFeatures.contains(featureFlagChangeDTO.getFeatureName());
    }
    return false;
  }
}
