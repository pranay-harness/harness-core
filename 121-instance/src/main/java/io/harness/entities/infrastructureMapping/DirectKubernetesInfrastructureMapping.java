package io.harness.entities.infrastructureMapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(HarnessTeam.DX)
@Data
@EqualsAndHashCode(callSuper = true)
public class DirectKubernetesInfrastructureMapping extends InfrastructureMapping {
  private String namespace;
  private String releaseName;
  private String clusterName;

  @Builder
  public DirectKubernetesInfrastructureMapping(String namespace, String releaseName, String clusterName) {
    this.namespace = namespace;
    this.releaseName = releaseName;
    this.clusterName = clusterName;
  }
}
