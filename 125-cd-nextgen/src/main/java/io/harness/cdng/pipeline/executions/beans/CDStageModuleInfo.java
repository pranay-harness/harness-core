package io.harness.cdng.pipeline.executions.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngpipeline.pipeline.executions.beans.ServiceExecutionSummary;
import io.harness.pms.contracts.cd.CDStageModuleInfoProto;
import io.harness.pms.contracts.service.StageModuleInfoProto;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
public class CDStageModuleInfo implements StageModuleInfo {
  ServiceExecutionSummary serviceInfo;
  InfraExecutionSummary infraExecutionSummary;
  String nodeExecutionId;

  @Override
  public StageModuleInfoProto toProto() {
    CDStageModuleInfoProto.Builder builder = CDStageModuleInfoProto.newBuilder();
    if (serviceInfo != null) {
      builder.setServiceInfo(serviceInfo.toProto());
    }
    if (infraExecutionSummary != null) {
      builder.setInfraInfo(infraExecutionSummary.toProto());
    }
    if (nodeExecutionId != null) {
      builder.setNodeExecutionId(nodeExecutionId);
    }
    return StageModuleInfoProto.newBuilder().setCdStageModuleInfo(builder.build()).build();
  }
}
