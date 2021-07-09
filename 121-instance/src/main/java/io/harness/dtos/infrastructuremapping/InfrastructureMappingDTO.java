package io.harness.dtos.infrastructuremapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.DX)
@Data
@NoArgsConstructor
public abstract class InfrastructureMappingDTO {
  private String id;
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String infrastructureMappingType;
  private String connectorRef;
  private String envId;
  private String deploymentType;
  private String serviceId;

  public InfrastructureMappingDTO(String infrastructureMappingType) {
    this.infrastructureMappingType = infrastructureMappingType;
  }
}
