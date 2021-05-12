package io.harness.models;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.DeploymentSummary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@OwnedBy(HarnessTeam.DX)
@EqualsAndHashCode(callSuper = false)
public class DeploymentEvent {
  private DeploymentSummary deploymentSummary;
  private RollbackInfo rollbackInfo;
}
