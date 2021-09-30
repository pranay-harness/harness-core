package io.harness.dtos.instancesyncperpetualtaskinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;

import lombok.Builder;
import lombok.Getter;

@OwnedBy(HarnessTeam.DX)
@Getter
@Builder
public class DeploymentInfoDetailsDTO {
  DeploymentInfoDTO deploymentInfoDTO;
  long lastUsedAt;

  public void setLastUsedAt(long lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }
}
