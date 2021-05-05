package io.harness.dto.infrastructureMapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.DX)
public abstract class InfrastructureMapping implements PersistentEntity {
  private String id;
  private String accountId;
  private String orgId;
  private String projectId;
  private String infraMappingType;
  private String connectorType;
  private String connectorId;
  private String envId;
  private String deploymentType;
  private String serviceId;
  private String infrastructureDefinitionId;
}
