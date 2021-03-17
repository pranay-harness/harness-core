package io.harness.perpetualtask;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._420_DELEGATE_AGENT)
public class PerpetualTaskAssignRecord {
  PerpetualTaskHandle perpetualTaskHandle;
  PerpetualTaskAssignDetails perpetualTaskAssignDetails;
}
