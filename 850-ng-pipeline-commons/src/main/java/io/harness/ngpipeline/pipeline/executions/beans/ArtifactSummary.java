package io.harness.ngpipeline.pipeline.executions.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface ArtifactSummary {
  String getType();
  String getDisplayName();
}
