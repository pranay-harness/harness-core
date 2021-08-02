package io.harness.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.entities.ArtifactDetails;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(HarnessTeam.DX)
@Getter
@Builder
public class DeploymentSummaryDTO {
  String id;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineExecutionId;
  String pipelineExecutionName;
  // TODO create dto for artifact details
  @Setter ArtifactDetails artifactDetails;
  String deployedById;
  String deployedByName;
  String infrastructureMappingId;
  @Setter @Nullable InfrastructureMappingDTO infrastructureMapping;
  // list of newly created server instances in fresh deployment
  @Setter List<ServerInstanceInfo> serverInstanceInfoList;
  DeploymentInfoDTO deploymentInfoDTO;
  long deployedAt;
  long createdAt;
  long lastModifiedAt;
}
