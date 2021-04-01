package io.harness.pms.sdk.execution.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.service.StageModuleInfoProto;

@OwnedBy(HarnessTeam.PIPELINE)
public interface StageModuleInfo {
  default StageModuleInfoProto toProto() {
    return null;
  }
}
