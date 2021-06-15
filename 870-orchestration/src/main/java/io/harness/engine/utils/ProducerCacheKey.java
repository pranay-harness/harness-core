package io.harness.engine.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
class ProducerCacheKey {
  enum EventCategory { INTERRUPT_EVENT, ORCHESTRATION_EVENT, FACILITATOR_EVENT, NODE_START, PROGRESS_EVENT }
  String serviceName;
  EventCategory eventCategory;
}
