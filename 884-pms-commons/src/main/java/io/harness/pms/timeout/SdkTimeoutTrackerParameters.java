package io.harness.pms.timeout;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.TimeoutParameters;

@OwnedBy(HarnessTeam.PIPELINE)
public interface SdkTimeoutTrackerParameters {
  TimeoutParameters prepareTimeoutParameters();
}
