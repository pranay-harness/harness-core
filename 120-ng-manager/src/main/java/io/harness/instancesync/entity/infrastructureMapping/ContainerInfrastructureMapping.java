package io.harness.instancesync.entity.infrastructureMapping;

import lombok.Data;

@Data
public abstract class ContainerInfrastructureMapping extends InfrastructureMapping {
  private String clusterName;
}
