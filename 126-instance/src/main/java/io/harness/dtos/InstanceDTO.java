package io.harness.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.instanceinfo.InstanceInfoDTO;
import io.harness.entities.ArtifactDetails;
import io.harness.entities.InstanceType;
import io.harness.ng.core.environment.beans.EnvironmentType;

import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class InstanceDTO {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String instanceKey;
  InstanceType instanceType;
  String envIdentifier;
  String envName;
  EnvironmentType envType;
  String serviceIdentifier;
  String serviceName;
  String infrastructureMappingId;
  String infrastructureKind;
  String connectorRef;
  ArtifactDetails primaryArtifact;
  String lastDeployedById;
  String lastDeployedByName;
  @NonFinal @Setter long lastDeployedAt;
  String lastPipelineExecutionId;
  String lastPipelineExecutionName;
  InstanceInfoDTO instanceInfoDTO;
  boolean isDeleted;
  long deletedAt;
  long createdAt;
  long lastModifiedAt;
}
