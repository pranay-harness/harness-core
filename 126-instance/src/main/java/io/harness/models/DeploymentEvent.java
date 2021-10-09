package io.harness.models;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.dtos.DeploymentSummaryDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@OwnedBy(HarnessTeam.DX)
@EqualsAndHashCode(callSuper = false)
public class DeploymentEvent {
  private DeploymentSummaryDTO deploymentSummaryDTO;
  private RollbackInfo rollbackInfo;
  private InfrastructureOutcome infrastructureOutcome;
}
