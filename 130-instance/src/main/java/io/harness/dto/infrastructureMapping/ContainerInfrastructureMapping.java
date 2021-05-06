package io.harness.dto.infrastructureMapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@OwnedBy(HarnessTeam.DX)
public abstract class ContainerInfrastructureMapping extends InfrastructureMapping {
  private String clusterName;
}
