package io.harness.entities.instancesyncperpetualtaskinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.deploymentinfo.DeploymentInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.DX)
public class DeploymentInfoDetails {
  DeploymentInfo deploymentInfo;
  long lastUsedAt;
}
