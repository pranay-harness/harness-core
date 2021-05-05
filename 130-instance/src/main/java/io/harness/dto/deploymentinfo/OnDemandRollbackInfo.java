package io.harness.dto.deploymentinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.DX)
public class OnDemandRollbackInfo {
  private boolean onDemandRollback;
  private String rollbackExecutionId;
}
