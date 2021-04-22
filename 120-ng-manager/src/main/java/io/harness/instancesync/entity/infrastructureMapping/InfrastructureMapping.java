package io.harness.instancesync.entity.infrastructureMapping;

import lombok.Data;

@Data
public abstract class InfrastructureMapping {
  private String accountId;
  private String infraMappingType;
  private String connectorType;
  private String connectorId;
  private String envId;
  private String deploymentType;
  private String serviceId;
  private String infrastructureDefinitionId;
}
