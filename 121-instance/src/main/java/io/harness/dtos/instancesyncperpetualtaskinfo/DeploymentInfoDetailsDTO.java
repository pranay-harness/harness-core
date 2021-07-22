package io.harness.dtos.instancesyncperpetualtaskinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class DeploymentInfoDetailsDTO {
  DeploymentInfoDTO deploymentInfoDTO;
  String deploymentSummaryId;
  long lastUsedAt;
}
