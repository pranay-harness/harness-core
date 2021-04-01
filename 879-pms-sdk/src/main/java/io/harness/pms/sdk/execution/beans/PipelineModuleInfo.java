package io.harness.pms.sdk.execution.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.service.PipelineModuleInfoProto;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PipelineModuleInfo {
  default PipelineModuleInfoProto toProto() {
    return null;
  }
}
