package io.harness.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@OwnedBy(HarnessTeam.DX)
@Data
@Builder
public class InfrastructureMappingDTO {
  private String id;
  @NonNull private String accountIdentifier;
  @NonNull private String orgIdentifier;
  @NonNull private String projectIdentifier;
  @NonNull private String infrastructureMappingType;
  @NonNull private String connectorRef;
  @NonNull private String envId;
  @NonNull private String deploymentType;
  @NonNull private String serviceId;
  @NonNull private String infrastructureId;
}
