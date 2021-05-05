package io.harness.dto.infrastructureMapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.DX)
public class DirectKubernetesInfrastructureMapping extends ContainerInfrastructureMapping {
  private String namespace;
  private String releaseName;
}
