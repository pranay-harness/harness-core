package io.harness.pms.sdk.core.steps.executables;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

@OwnedBy(HarnessTeam.PIPELINE)
public interface StepDetailsInfo {
  default String toViewJson() {
    return RecastOrchestrationUtils.toJson(this);
  }
}
