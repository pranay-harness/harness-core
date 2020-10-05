package io.harness.cdng.pipeline.executions.beans;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.Value;

import java.util.List;

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
