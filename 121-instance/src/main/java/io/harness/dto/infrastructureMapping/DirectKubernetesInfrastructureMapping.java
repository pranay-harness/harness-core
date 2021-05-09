package io.harness.dto.infrastructureMapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.DX)
public class DirectKubernetesInfrastructureMapping extends ContainerInfrastructureMapping {
  private String namespace;
  private String releaseName;

  @Builder
  public DirectKubernetesInfrastructureMapping(String id, String accountId, String orgId, String projectId,
      String infraMappingType, String connectorType, String connectorId, String envId, String deploymentType,
      String serviceId, String clusterName, String namespace, String releaseName) {
    super(id, accountId, orgId, projectId, infraMappingType, connectorType, connectorId, envId, deploymentType,
        serviceId, clusterName);
    this.namespace = namespace;
    this.releaseName = releaseName;
  }
}
