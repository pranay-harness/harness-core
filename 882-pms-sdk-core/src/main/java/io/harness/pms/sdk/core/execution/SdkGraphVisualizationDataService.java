package io.harness.pms.sdk.core.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.StepDetailsInfo;

@OwnedBy(HarnessTeam.PIPELINE)
public interface SdkGraphVisualizationDataService {
  void publishStepDetailInformation(Ambiance ambiance, StepDetailsInfo stepDetailsInfo, String name);
}
