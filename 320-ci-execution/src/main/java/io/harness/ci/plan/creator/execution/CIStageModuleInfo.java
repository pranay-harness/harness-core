package io.harness.ci.plan.creator.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ci.CIStageModuleInfoProto;
import io.harness.pms.contracts.service.StageModuleInfoProto;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CI)
public class CIStageModuleInfo implements StageModuleInfo {
  String nodeExecutionId;

  @Override
  public StageModuleInfoProto toProto() {
    CIStageModuleInfoProto.Builder builder = CIStageModuleInfoProto.newBuilder();
    if (nodeExecutionId != null) {
      builder.setNodeExecutionId(nodeExecutionId);
    }
    return StageModuleInfoProto.newBuilder().setCiStageModuleInfo(builder.build()).build();
  }
}
