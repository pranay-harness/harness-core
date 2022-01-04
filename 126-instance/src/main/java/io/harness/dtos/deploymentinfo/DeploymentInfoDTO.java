package io.harness.dtos.deploymentinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode
@OwnedBy(HarnessTeam.DX)
public abstract class DeploymentInfoDTO {
  // Create combination of fields that can be used to identify related instance info details
  // The key should be same as instance handler key of the corresponding instance info
  public abstract String getType();
  public abstract String prepareInstanceSyncHandlerKey();
}
