package io.harness.instancesync.dto.infrastructureMapping;

import io.harness.persistence.PersistentEntity;

import lombok.Data;

@Data
public abstract class InfrastructureMapping implements PersistentEntity {
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
