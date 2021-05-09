package io.harness.dto.infrastructureMapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.DX)
public abstract class ContainerInfrastructureMapping extends InfrastructureMapping {
  private String clusterName;

  @Builder
  public ContainerInfrastructureMapping(String id, String accountId, String orgId, String projectId,
      String infraMappingType, String connectorType, String connectorId, String envId, String deploymentType,
      String serviceId, String infrastructureDefinitionId, String clusterName) {
    super(id, accountId, orgId, projectId, infraMappingType, connectorType, connectorId, envId, deploymentType,
        serviceId, infrastructureDefinitionId);
    this.clusterName = clusterName;
  }
}
