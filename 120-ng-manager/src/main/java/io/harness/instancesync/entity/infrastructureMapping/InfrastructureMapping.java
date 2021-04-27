package io.harness.instancesync.entity.infrastructureMapping;

import lombok.Data;

@Data
public abstract class InfrastructureMapping {
  // This id is created using hash of service-env- and other fields and should remain consistent
  // for this combination of fields
  private String id;
  private String accountId;
  private String infraMappingType;
  private String connectorType;
  private String connectorId;
  private String envId;
  private String deploymentType;
  private String serviceId;
  private String infrastructureDefinitionId;
}
