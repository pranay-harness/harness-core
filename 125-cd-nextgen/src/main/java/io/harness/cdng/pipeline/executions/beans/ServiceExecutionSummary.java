package io.harness.cdng.pipeline.executions.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngpipeline.pipeline.executions.beans.ArtifactSummary;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@RecasterAlias("io.harness.cdng.pipeline.executions.beans.ServiceExecutionSummary")
@OwnedBy(HarnessTeam.CDP)
public class ServiceExecutionSummary {
  String identifier;
  String displayName;
  String deploymentType;
  ArtifactsSummary artifacts;

  @Data
  @Builder
  @RecasterAlias("io.harness.cdng.pipeline.executions.beans.ServiceExecutionSummary$ArtifactsSummary")
  public static class ArtifactsSummary {
    private ArtifactSummary primary;
    @Singular private List<ArtifactSummary> sidecars;
  }
}
