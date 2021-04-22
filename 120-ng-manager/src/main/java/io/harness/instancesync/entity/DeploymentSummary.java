package io.harness.instancesync.entity;

import io.harness.instancesync.entity.deploymentinfo.DeploymentInfo;
import io.harness.instancesync.entity.deploymentinfo.OnDemandRollbackInfo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class DeploymentSummary {
  private String accountId;
  private String pipelineExecutionId;
  private String pipelineExecutionName;
  private String artifactId;
  private String artifactName;
  private String artifactBuildNum;
  private String deployedById;
  private String deployedByName;
  private long deployedAt;
  private DeploymentInfo deploymentInfo;

  private boolean isRollback;
  private OnDemandRollbackInfo onDemandRollbackInfo;
}
