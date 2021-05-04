package io.harness.entity;

import io.harness.entity.deploymentinfo.OnDemandRollbackInfo;
import io.harness.queue.Queuable;

import lombok.Data;

@Data
public class DeploymentEvent extends Queuable {
  private boolean isRollback;
  private DeploymentSummary deploymentSummary;
  private OnDemandRollbackInfo onDemandRollbackInfo;
}
