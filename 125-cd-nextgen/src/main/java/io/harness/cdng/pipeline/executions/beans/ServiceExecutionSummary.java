/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.pipeline.executions.beans;

import io.harness.ngpipeline.pipeline.executions.beans.ArtifactSummary;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ServiceExecutionSummary {
  String identifier;
  String displayName;
  String deploymentType;
  ArtifactsSummary artifacts;

  @Data
  @Builder
  public static class ArtifactsSummary {
    private ArtifactSummary primary;
    @Singular private List<ArtifactSummary> sidecars;
  }
}
