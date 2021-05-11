package io.harness.dto.infrastructureMapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.DX)
public class DirectKubernetesInfrastructureMapping extends InfrastructureMapping {
  private String namespace;
  private String releaseName;
  private String clusterName;

  @Builder
  public DirectKubernetesInfrastructureMapping(String id, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String infrastructureMappingType, String connectorRef, String envId,
      String deploymentType, String serviceId, String namespace, String releaseName, String clusterName) {
    super(id, accountIdentifier, orgIdentifier, projectIdentifier, infrastructureMappingType, connectorRef, envId,
        deploymentType, serviceId);
    this.namespace = namespace;
    this.releaseName = releaseName;
    this.clusterName = clusterName;
  }
}
