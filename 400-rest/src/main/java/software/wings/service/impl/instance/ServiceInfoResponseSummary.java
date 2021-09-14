/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class ServiceInfoResponseSummary {
  String lastArtifactBuildNum;
  String lastWorkflowExecutionId;
  String lastWorkflowExecutionName;
  String infraMappingId;
  String infraMappingName;
}
