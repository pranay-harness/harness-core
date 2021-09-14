/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.beans.WorkflowType;

import software.wings.beans.artifact.Artifact;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LastDeployedArtifactInformation {
  Artifact artifact;
  Long executionStartTime;
  String envId;
  String executionId;
  String executionEntityId;
  WorkflowType executionEntityType;
  String executionEntityName;
}
