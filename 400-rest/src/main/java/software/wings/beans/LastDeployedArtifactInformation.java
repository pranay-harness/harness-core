/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.WorkflowType;

import software.wings.beans.artifact.Artifact;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class LastDeployedArtifactInformation {
  Artifact artifact;
  Long executionStartTime;
  String envId;
  String executionId;
  String executionEntityId;
  WorkflowType executionEntityType;
  String executionEntityName;
}
