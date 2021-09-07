package io.harness.engine.pms.commons.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.events.base.PmsEventCategory;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
class ProducerCacheKey {
  String serviceName;
  PmsEventCategory eventCategory;
}
