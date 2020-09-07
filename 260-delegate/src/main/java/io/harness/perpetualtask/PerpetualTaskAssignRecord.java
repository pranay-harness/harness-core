package io.harness.perpetualtask;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerpetualTaskAssignRecord {
  PerpetualTaskHandle perpetualTaskHandle;
  PerpetualTaskAssignDetails perpetualTaskAssignDetails;
}
