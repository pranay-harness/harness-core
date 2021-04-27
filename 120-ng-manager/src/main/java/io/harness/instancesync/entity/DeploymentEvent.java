package io.harness.instancesync.entity;

import io.harness.instancesync.entity.deploymentinfo.OnDemandRollbackInfo;
import io.harness.queue.Queuable;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
public class DeploymentEvent extends Queuable {
  private boolean isRollback;
  private DeploymentSummary deploymentSummary;
  private OnDemandRollbackInfo onDemandRollbackInfo;
}
