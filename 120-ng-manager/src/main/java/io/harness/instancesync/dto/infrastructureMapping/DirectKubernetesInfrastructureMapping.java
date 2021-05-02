package io.harness.instancesync.dto.infrastructureMapping;

import lombok.Data;

@Data
public class DirectKubernetesInfrastructureMapping extends ContainerInfrastructureMapping {
  private String namespace;
  private String releaseName;
}
