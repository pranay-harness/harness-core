package io.harness.entity;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dto.DeploymentSummary;
import io.harness.dto.deploymentinfo.OnDemandRollbackInfo;
import io.harness.queue.Queuable;

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.DX)
public class DeploymentEvent extends Queuable {
  private boolean isRollback;
  private DeploymentSummary deploymentSummary;
  private OnDemandRollbackInfo onDemandRollbackInfo;
}
