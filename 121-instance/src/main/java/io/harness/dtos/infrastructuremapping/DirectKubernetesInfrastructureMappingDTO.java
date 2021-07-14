package io.harness.dtos.infrastructuremapping;

import lombok.Data;

@Data
@io.harness.annotations.dev.OwnedBy(io.harness.annotations.dev.HarnessTeam.DX)
public class DirectKubernetesInfrastructureMappingDTO extends InfrastructureMappingDTO {
  private String namespace;
  private String releaseName;
  private String clusterName;

  public DirectKubernetesInfrastructureMappingDTO() {
    // TODO Use an enum of infrastructureMappingType and put its reference here
    super("infrastructureMappingTypePlaceholder");
  }
}
