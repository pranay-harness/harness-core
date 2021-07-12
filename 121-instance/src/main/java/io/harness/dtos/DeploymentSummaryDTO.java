package io.harness.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.dtos.infrastructuremapping.InfrastructureMappingDTO;
import io.harness.entities.ArtifactDetails;

import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class DeploymentSummaryDTO {
  String id;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineExecutionId;
  String pipelineExecutionName;
  // TODO create dto for artifact details
  ArtifactDetails artifactDetails;
  String deployedById;
  String deployedByName;
  String infrastructureMappingId;
  @Nullable InfrastructureMappingDTO infrastructureMapping;
  DeploymentInfoDTO deploymentInfoDTO;
  long deployedAt;
  long createdAt;
  long lastModifiedAt;
}
