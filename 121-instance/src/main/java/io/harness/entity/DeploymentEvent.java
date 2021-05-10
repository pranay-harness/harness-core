package io.harness.entity;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dto.DeploymentSummary;
import io.harness.dto.deploymentinfo.RollbackInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
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
