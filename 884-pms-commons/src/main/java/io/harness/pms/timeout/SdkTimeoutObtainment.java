package io.harness.pms.timeout;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.contracts.Dimension;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
public class SdkTimeoutObtainment {
  Dimension dimension;
  SdkTimeoutTrackerParameters parameters;
}
