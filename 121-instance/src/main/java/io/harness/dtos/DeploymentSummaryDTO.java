/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
  String instanceSyncKey;
  @Setter @Nullable InfrastructureMappingDTO infrastructureMapping;
  // status of server instances when deployment happened
  // only used during new deployment instance sync flow
  @Setter List<ServerInstanceInfo> serverInstanceInfoList;
  DeploymentInfoDTO deploymentInfoDTO;
  @Setter boolean isRollbackDeployment;
  long deployedAt;
  long createdAt;
  long lastModifiedAt;
}
